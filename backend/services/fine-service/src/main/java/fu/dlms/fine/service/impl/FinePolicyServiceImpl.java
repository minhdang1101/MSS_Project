package fu.dlms.fine.service.impl;

import fu.dlms.fine.dto.FinePolicyRequest;
import fu.dlms.fine.dto.FinePolicyResponse;
import fu.dlms.fine.entity.FinePolicy;
import fu.dlms.fine.exception.ResourceNotFoundException;
import fu.dlms.fine.repository.FinePolicyRepository;
import fu.dlms.fine.service.FineCalculator;
import fu.dlms.fine.service.FinePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinePolicyServiceImpl implements FinePolicyService {

    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FinePolicyRepository finePolicyRepository;
    private final Clock clock;

    @Override
    public FinePolicyResponse findCurrent() {
        LocalDate today = today();
        return finePolicyRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(today)
                .map(FinePolicyResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có chính sách phạt nào áp dụng cho ngày "
                        + today.format(VN_DATE) + "."));
    }

    @Override
    @Transactional
    public FinePolicyResponse update(FinePolicyRequest request) {
        // At most one active version at a time (unique index in fine_db): switch the old one off first.
        finePolicyRepository.findByActiveTrue().forEach(old -> old.setActive(false));
        finePolicyRepository.flush();

        FinePolicy policy = new FinePolicy();
        policy.setDailyRate(request.dailyRate());
        policy.setGraceDays(request.graceDays());
        policy.setMaxAmount(request.maxAmount());
        policy.setEffectiveFrom(request.effectiveFrom());
        policy.setActive(true);
        return FinePolicyResponse.from(finePolicyRepository.save(policy));
    }

    private LocalDate today() {
        return FineCalculator.libraryDate(clock.instant());
    }
}
