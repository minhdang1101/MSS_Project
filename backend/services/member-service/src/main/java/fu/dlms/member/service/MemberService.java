package fu.dlms.member.service;

import fu.dlms.member.dto.MemberRequest;
import fu.dlms.member.dto.MemberResponse;

import java.util.List;

public interface MemberService {

    List<MemberResponse> findAll();

    MemberResponse findById(Long id);

    MemberResponse create(MemberRequest request);
}
