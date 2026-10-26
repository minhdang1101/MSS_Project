package fu.dlms.fine.service.impl;

import fu.dlms.fine.dto.FineResponse;
import fu.dlms.fine.entity.Fine;
import fu.dlms.fine.entity.FineStatus;
import fu.dlms.fine.exception.BusinessException;
import fu.dlms.fine.exception.ResourceNotFoundException;
import fu.dlms.fine.repository.FineRepository;
import fu.dlms.fine.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FineServiceImpl implements FineService {

    private final FineRepository fineRepository;

    @Override
    public List<FineResponse> findAll() {
        return fineRepository.findAll().stream().map(FineResponse::from).toList();
    }

    @Override
    public List<FineResponse> findByMember(Long memberId) {
        return fineRepository.findByMemberId(memberId).stream().map(FineResponse::from).toList();
    }

    @Override
    @Transactional
    public FineResponse pay(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine", fineId));
        if (fine.getStatus() != FineStatus.UNPAID) {
            throw new BusinessException("Fine " + fineId + " is already " + fine.getStatus());
        }
        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(Instant.now());
        return FineResponse.from(fine);
    }
}
