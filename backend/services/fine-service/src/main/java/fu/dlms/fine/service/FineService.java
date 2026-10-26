package fu.dlms.fine.service;

import fu.dlms.fine.dto.FineResponse;

import java.util.List;

public interface FineService {

    List<FineResponse> findAll();

    List<FineResponse> findByMember(Long memberId);

    FineResponse pay(Long fineId);
}
