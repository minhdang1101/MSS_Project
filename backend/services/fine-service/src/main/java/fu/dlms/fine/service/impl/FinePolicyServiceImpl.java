package fu.dlms.fine.service.impl;

import fu.dlms.fine.dto.ClosedDayRequest;
import fu.dlms.fine.dto.ClosedDayResponse;
import fu.dlms.fine.dto.FinePolicyRequest;
import fu.dlms.fine.dto.FinePolicyResponse;
import fu.dlms.fine.entity.ClosedDay;
import fu.dlms.fine.entity.FinePolicy;
import fu.dlms.fine.exception.BusinessException;
import fu.dlms.fine.exception.ResourceNotFoundException;
import fu.dlms.fine.repository.ClosedDayRepository;
import fu.dlms.fine.repository.FinePolicyRepository;
import fu.dlms.fine.service.FineCalculator;
import fu.dlms.fine.service.FinePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinePolicyServiceImpl implements FinePolicyService {

    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FinePolicyRepository finePolicyRepository;
    private final ClosedDayRepository closedDayRepository;
    private final Clock clock;

    @Override
    public List<FinePolicyResponse> findAll() {
        return finePolicyRepository.findAllByOrderByEffectiveFromDescIdDesc().stream()
                .map(FinePolicyResponse::from).toList();
    }

    @Override
    public FinePolicyResponse findCurrent() {
        LocalDate today = today();
        return finePolicyRepository.findFirstByActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(today)
                .map(FinePolicyResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có chính sách phạt nào áp dụng cho ngày "
                        + today.format(VN_DATE) + "."));
    }

    @Override
    @Transactional
    public FinePolicyResponse create(FinePolicyRequest request) {
        FinePolicy policy = new FinePolicy();
        policy.setDailyRate(request.dailyRate());
        policy.setGraceDays(request.graceDays());
        policy.setMaxAmount(request.maxAmount());
        policy.setLostBookFee(request.lostBookFee());
        policy.setEffectiveFrom(request.effectiveFrom());
        policy.setActive(true);
        return FinePolicyResponse.from(finePolicyRepository.save(policy));
    }

    @Override
    @Transactional
    public FinePolicyResponse deactivate(Long id) {
        FinePolicy policy = finePolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinePolicy", id));
        policy.setActive(false);
        // The query flushes the change first, so it answers "what would apply today without this version?"
        if (finePolicyRepository.findFirstByActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(today())
                .isEmpty()) {
            throw new BusinessException("Không thể ngừng áp dụng: hôm nay sẽ không còn chính sách phạt nào. "
                    + "Tạo chính sách mới trước.");
        }
        return FinePolicyResponse.from(policy);
    }

    @Override
    public List<ClosedDayResponse> findClosedDays() {
        return closedDayRepository.findAllByOrderByDateAsc().stream().map(ClosedDayResponse::from).toList();
    }

    @Override
    @Transactional
    public ClosedDayResponse addClosedDay(ClosedDayRequest request) {
        if (closedDayRepository.existsById(request.date())) {
            throw new BusinessException("Ngày " + request.date().format(VN_DATE) + " đã có trong lịch nghỉ.");
        }
        ClosedDay day = new ClosedDay();
        day.setDate(request.date());
        day.setReason(request.reason() == null || request.reason().isBlank() ? null : request.reason().trim());
        return ClosedDayResponse.from(closedDayRepository.save(day));
    }

    @Override
    @Transactional
    public void removeClosedDay(LocalDate date) {
        if (!closedDayRepository.existsById(date)) {
            throw new ResourceNotFoundException("Ngày " + date.format(VN_DATE) + " không có trong lịch nghỉ.");
        }
        closedDayRepository.deleteById(date);
    }

    private LocalDate today() {
        return FineCalculator.libraryDate(clock.instant());
    }
}
