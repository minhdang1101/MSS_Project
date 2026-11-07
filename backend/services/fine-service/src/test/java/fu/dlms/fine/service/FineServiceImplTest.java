package fu.dlms.fine.service;

import fu.dlms.fine.dto.FineResponse;
import fu.dlms.fine.entity.ClosedDay;
import fu.dlms.fine.entity.Fine;
import fu.dlms.fine.entity.FinePolicy;
import fu.dlms.fine.entity.FineStatus;
import fu.dlms.fine.exception.BusinessException;
import fu.dlms.fine.messaging.LoanEvent;
import fu.dlms.fine.repository.ClosedDayRepository;
import fu.dlms.fine.repository.FinePolicyRepository;
import fu.dlms.fine.repository.FineRepository;
import fu.dlms.fine.service.impl.FineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FineServiceImplTest {

    /** 27/10/2026 10:00 in Hà Nội. */
    private static final Instant NOW = Instant.parse("2026-10-27T03:00:00Z");

    private final FineRepository fineRepository = mock(FineRepository.class);
    private final FinePolicyRepository policyRepository = mock(FinePolicyRepository.class);
    private final ClosedDayRepository closedDayRepository = mock(ClosedDayRepository.class);
    private final FinePolicy policy = FineCalculatorTest.policy(3000, 1, 150000);
    private FineServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FineServiceImpl(fineRepository, policyRepository, closedDayRepository, new FineCalculator(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(fineRepository.save(any(Fine.class))).thenAnswer(inv -> inv.getArgument(0));
        when(policyRepository.findFirstByActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(any()))
                .thenReturn(Optional.of(policy));
        when(closedDayRepository.findAll()).thenReturn(List.of());
    }

    private static Instant daysAgo(int days) {
        return NOW.minus(days, ChronoUnit.DAYS);
    }

    private static LoanEvent event(long loanId, String loanType, Instant dueAt, Instant returnedAt) {
        return new LoanEvent("evt-" + loanId, "LOAN_EVENT", loanId, 7L, 3L, 30L, loanType,
                dueAt.minus(14, ChronoUnit.DAYS), dueAt, returnedAt, returnedAt != null ? returnedAt : NOW);
    }

    private Fine fine(long id, FineStatus status, Instant dueAt, int days, int amount) {
        Fine fine = new Fine();
        fine.setId(id);
        fine.setLoanId(100 + id);
        fine.setMemberId(7L);
        fine.setBookId(3L);
        fine.setDueAt(dueAt);
        fine.setPolicy(policy);
        fine.setStatus(status);
        fine.setOverdueDays(days);
        fine.setDailyRate(policy.getDailyRate());
        fine.setAmount(BigDecimal.valueOf(amount));
        fine.setCalculatedAt(daysAgo(30));
        return fine;
    }

    private Fine savedFine() {
        ArgumentCaptor<Fine> captor = ArgumentCaptor.forClass(Fine.class);
        verify(fineRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---------- LoanOverdue ----------

    @Test
    void overdueOpensAnAccruingFineWithTheCurrentPolicy() {
        service.onLoanOverdue(event(1, "PHYSICAL", daysAgo(3), null));

        Fine fine = savedFine();
        assertThat(fine.getStatus()).isEqualTo(FineStatus.ACCRUING);
        assertThat(fine.getLoanId()).isEqualTo(1L);
        assertThat(fine.getBookId()).isEqualTo(3L);
        assertThat(fine.getPolicy()).isSameAs(policy);
        assertThat(fine.getOverdueDays()).isEqualTo(3);
        assertThat(fine.getAmount()).isEqualByComparingTo("6000");
    }

    @Test
    void aSecondOverdueEventForTheSameLoanChangesNothing() {
        when(fineRepository.findByLoanId(1L)).thenReturn(Optional.of(fine(1, FineStatus.ACCRUING, daysAgo(3), 1, 0)));

        service.onLoanOverdue(event(1, "PHYSICAL", daysAgo(3), null));

        verify(fineRepository, never()).save(any());
    }

    @Test
    void digitalLoansAreNeverFined() {
        service.onLoanOverdue(event(1, "DIGITAL", daysAgo(10), null));
        service.onLoanReturned(event(1, "DIGITAL", daysAgo(10), NOW));

        verify(fineRepository, never()).save(any());
    }

    @Test
    void anEventWithoutADueDateIsRejectedSoItLandsInTheDeadLetterQueue() {
        LoanEvent broken = new LoanEvent("evt", "LOAN_EVENT", 1L, 7L, 3L, null, "PHYSICAL", NOW, null, null, NOW);

        assertThatThrownBy(() -> service.onLoanOverdue(broken)).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- LoanReturned ----------

    @Test
    void returnSettlesTheAccruingFineAsUnpaidAtTheDayOfReturn() {
        Fine accruing = fine(1, FineStatus.ACCRUING, daysAgo(20), 1, 0);
        when(fineRepository.findByLoanId(101L)).thenReturn(Optional.of(accruing));

        service.onLoanReturned(event(101, "PHYSICAL", daysAgo(20), daysAgo(6)));

        Fine fine = savedFine();
        assertThat(fine).isSameAs(accruing);
        assertThat(fine.getStatus()).isEqualTo(FineStatus.UNPAID);
        assertThat(fine.getOverdueDays()).isEqualTo(14);
        assertThat(fine.getAmount()).isEqualByComparingTo("39000");
        assertThat(fine.getReturnedAt()).isEqualTo(daysAgo(6));
    }

    @Test
    void returnWithoutAnEarlierOverdueEventStillCreatesTheFine() {
        service.onLoanReturned(event(1, null, daysAgo(10), NOW));

        Fine fine = savedFine();
        assertThat(fine.getStatus()).isEqualTo(FineStatus.UNPAID);
        assertThat(fine.getAmount()).isEqualByComparingTo("27000");
    }

    @Test
    void returnWithinTheGracePeriodRemovesTheAccruingFine() {
        Fine accruing = fine(1, FineStatus.ACCRUING, daysAgo(1), 1, 0);
        when(fineRepository.findByLoanId(101L)).thenReturn(Optional.of(accruing));

        service.onLoanReturned(event(101, "PHYSICAL", daysAgo(1), NOW));

        verify(fineRepository).delete(accruing);
        verify(fineRepository, never()).save(any());
    }

    @Test
    void returnOnTimeCreatesNothing() {
        service.onLoanReturned(event(1, "PHYSICAL", NOW, NOW));

        verify(fineRepository, never()).save(any());
        verify(fineRepository, never()).delete(any());
    }

    @Test
    void aDuplicateReturnEventDoesNotReopenAPaidFine() {
        Fine paid = fine(1, FineStatus.PAID, daysAgo(20), 14, 39000);
        when(fineRepository.findByLoanId(101L)).thenReturn(Optional.of(paid));

        service.onLoanReturned(event(101, "PHYSICAL", daysAgo(20), NOW));

        assertThat(paid.getStatus()).isEqualTo(FineStatus.PAID);
        assertThat(paid.getAmount()).isEqualByComparingTo("39000");
        verify(fineRepository, never()).save(any());
    }

    @Test
    void closedDaysAreLeftOutWhenTheFineIsSettled() {
        LocalDate holiday = LocalDate.of(2026, 10, 20);
        ClosedDay closed = new ClosedDay();
        closed.setDate(holiday);
        when(closedDayRepository.findAll()).thenReturn(List.of(closed));

        service.onLoanReturned(event(1, "PHYSICAL", daysAgo(10), NOW));

        assertThat(savedFine().getOverdueDays()).isEqualTo(9);
    }

    // ---------- listing ----------

    @Test
    void accruingFinesAreWorkedOutUpToTodayAndListedFirst() {
        Fine settled = fine(1, FineStatus.UNPAID, daysAgo(40), 2, 3000);
        settled.setCalculatedAt(daysAgo(1));
        Fine accruing = fine(2, FineStatus.ACCRUING, daysAgo(43), 1, 0); // stored values are from day one
        when(fineRepository.findAll()).thenReturn(List.of(settled, accruing));

        List<FineResponse> fines = service.findAll();

        assertThat(fines).extracting(FineResponse::id).containsExactly(2L, 1L);
        assertThat(fines.get(0).overdueDays()).isEqualTo(43);
        assertThat(fines.get(0).amount()).isEqualByComparingTo("126000");
        assertThat(fines.get(1).amount()).isEqualByComparingTo("3000");
    }

    // ---------- pay / waive ----------

    @Test
    void payMarksAnUnpaidFineAsPaidNow() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine(1, FineStatus.UNPAID, daysAgo(10), 10, 27000)));

        FineResponse paid = service.pay(1L);

        assertThat(paid.status()).isEqualTo(FineStatus.PAID);
        assertThat(paid.paidAt()).isEqualTo(NOW);
    }

    @Test
    void anAccruingFineCannotBeCollectedBeforeTheBookIsBack() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine(1, FineStatus.ACCRUING, daysAgo(10), 1, 0)));

        assertThatThrownBy(() -> service.pay(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa được trả");
    }

    @Test
    void aSettledFineCannotBeWaivedOrPaidAgain() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine(1, FineStatus.PAID, daysAgo(10), 10, 27000)));

        assertThatThrownBy(() -> service.waive(1L)).hasMessage("Khoản phạt này đã được thu.");
        assertThatThrownBy(() -> service.pay(1L)).hasMessage("Khoản phạt này đã được thu.");
    }

    @Test
    void waiveClosesAnUnpaidFineWithoutCollectingIt() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine(1, FineStatus.UNPAID, daysAgo(10), 10, 27000)));

        FineResponse waived = service.waive(1L);

        assertThat(waived.status()).isEqualTo(FineStatus.WAIVED);
        assertThat(waived.paidAt()).isNull();
    }
}
