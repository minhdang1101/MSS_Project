package fu.dlms.fine.service.impl;

import fu.dlms.fine.dto.FineResponse;
import fu.dlms.fine.entity.ClosedDay;
import fu.dlms.fine.entity.Fine;
import fu.dlms.fine.entity.FinePolicy;
import fu.dlms.fine.entity.FineStatus;
import fu.dlms.fine.exception.BusinessException;
import fu.dlms.fine.exception.ResourceNotFoundException;
import fu.dlms.fine.messaging.LoanEvent;
import fu.dlms.fine.repository.ClosedDayRepository;
import fu.dlms.fine.repository.FinePolicyRepository;
import fu.dlms.fine.repository.FineRepository;
import fu.dlms.fine.service.FineCalculator;
import fu.dlms.fine.service.FineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FineServiceImpl implements FineService {

    private static final String DIGITAL = "DIGITAL";
    private static final Comparator<FineResponse> NEWEST_FIRST =
            Comparator.comparing(FineResponse::calculatedAt).thenComparing(FineResponse::id).reversed();

    private final FineRepository fineRepository;
    private final FinePolicyRepository finePolicyRepository;
    private final ClosedDayRepository closedDayRepository;
    private final FineCalculator calculator;
    private final Clock clock;

    @Override
    public List<FineResponse> findAll() {
        return toResponses(fineRepository.findAll());
    }

    @Override
    public List<FineResponse> findByMember(Long memberId) {
        return toResponses(fineRepository.findByMemberId(memberId));
    }

    @Override
    @Transactional
    public FineResponse pay(Long fineId) {
        Fine fine = loadUnpaid(fineId);
        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(clock.instant());
        return FineResponse.from(fine);
    }

    @Override
    @Transactional
    public FineResponse waive(Long fineId) {
        Fine fine = loadUnpaid(fineId);
        fine.setStatus(FineStatus.WAIVED);
        return FineResponse.from(fine);
    }

    @Override
    @Transactional
    public void onLoanOverdue(LoanEvent event) {
        if (isNotFinable(event)) {
            return;
        }
        if (fineRepository.findByLoanId(event.loanId()).isPresent()) {
            log.debug("Loan {} already has a fine; LoanOverdue {} ignored", event.loanId(), event.eventId());
            return;
        }
        Fine fine = newFine(event);
        fine.setStatus(FineStatus.ACCRUING);
        recalculate(fine, clock.instant(), closedDays());
        fineRepository.save(fine);
    }

    @Override
    @Transactional
    public void onLoanReturned(LoanEvent event) {
        if (isNotFinable(event)) {
            return;
        }
        Optional<Fine> existing = fineRepository.findByLoanId(event.loanId());
        if (existing.isPresent() && existing.get().getStatus() != FineStatus.ACCRUING) {
            log.debug("Fine for loan {} is already settled; LoanReturned {} ignored", event.loanId(), event.eventId());
            return;
        }
        Instant returnedAt = event.returnedAt() != null ? event.returnedAt() : event.occurredAt();
        if (returnedAt == null) {
            throw new IllegalArgumentException("LoanReturned " + event.eventId() + " has neither returnedAt nor occurredAt");
        }

        // No LoanOverdue seen (returned before the overdue scan ran, or events reordered): settle in one step.
        Fine fine = existing.orElseGet(() -> newFine(event));
        recalculate(fine, returnedAt, closedDays());
        if (fine.getAmount().signum() == 0) {
            existing.ifPresent(fineRepository::delete); // back within the grace period: nothing owed
            return;
        }
        fine.setReturnedAt(returnedAt);
        fine.setStatus(FineStatus.UNPAID);
        fineRepository.save(fine);
    }

    private Fine loadUnpaid(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine", fineId));
        if (fine.getStatus() == FineStatus.ACCRUING) {
            throw new BusinessException("Sách chưa được trả nên tiền phạt vẫn đang tăng. "
                    + "Chỉ thu hoặc miễn phạt sau khi nhận lại sách.");
        }
        if (fine.getStatus() != FineStatus.UNPAID) {
            throw new BusinessException(fine.getStatus() == FineStatus.PAID
                    ? "Khoản phạt này đã được thu." : "Khoản phạt này đã được miễn.");
        }
        return fine;
    }

    /** Digital loans are never fined. Throws on an event too incomplete to act on, so it ends up in the DLQ. */
    private boolean isNotFinable(LoanEvent event) {
        if (event.loanId() == null || event.memberId() == null || event.bookId() == null || event.dueAt() == null) {
            throw new IllegalArgumentException("Loan event " + event.eventId() + " lacks loanId, memberId, bookId or dueAt");
        }
        if (DIGITAL.equalsIgnoreCase(event.loanType())) {
            log.debug("Loan {} is digital: no fine", event.loanId());
            return true;
        }
        return false;
    }

    private Fine newFine(LoanEvent event) {
        Fine fine = new Fine();
        fine.setLoanId(event.loanId());
        fine.setMemberId(event.memberId());
        fine.setBookId(event.bookId());
        fine.setDueAt(event.dueAt());
        fine.setPolicy(currentPolicy());
        return fine;
    }

    private FinePolicy currentPolicy() {
        LocalDate today = FineCalculator.libraryDate(clock.instant());
        return finePolicyRepository.findFirstByActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(today)
                .orElseThrow(() -> new IllegalStateException("No fine policy is in effect on " + today));
    }

    private void recalculate(Fine fine, Instant until, Set<LocalDate> closedDays) {
        int lateDays = calculator.lateDays(fine.getDueAt(), until, closedDays);
        fine.setOverdueDays(lateDays);
        fine.setDailyRate(fine.getPolicy().getDailyRate());
        fine.setAmount(calculator.amount(lateDays, fine.getPolicy()));
        fine.setCalculatedAt(clock.instant());
    }

    private List<FineResponse> toResponses(List<Fine> fines) {
        Instant now = clock.instant();
        Set<LocalDate> closedDays = fines.stream().anyMatch(f -> f.getStatus() == FineStatus.ACCRUING)
                ? closedDays() : Set.of();
        return fines.stream()
                .map(fine -> fine.getStatus() == FineStatus.ACCRUING ? accruedSoFar(fine, now, closedDays)
                        : FineResponse.from(fine))
                .sorted(NEWEST_FIRST)
                .toList();
    }

    private FineResponse accruedSoFar(Fine fine, Instant now, Set<LocalDate> closedDays) {
        int lateDays = calculator.lateDays(fine.getDueAt(), now, closedDays);
        return FineResponse.from(fine, lateDays, calculator.amount(lateDays, fine.getPolicy()), now);
    }

    /** The closed-day calendar is a handful of holidays a year, so it is read whole. */
    private Set<LocalDate> closedDays() {
        return closedDayRepository.findAll().stream().map(ClosedDay::getDate).collect(Collectors.toSet());
    }
}
