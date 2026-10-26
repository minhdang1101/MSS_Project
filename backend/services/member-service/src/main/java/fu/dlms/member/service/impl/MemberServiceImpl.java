package fu.dlms.member.service.impl;

import fu.dlms.member.dto.MemberRequest;
import fu.dlms.member.dto.MemberResponse;
import fu.dlms.member.entity.Member;
import fu.dlms.member.exception.BusinessException;
import fu.dlms.member.exception.ResourceNotFoundException;
import fu.dlms.member.repository.MemberRepository;
import fu.dlms.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    public List<MemberResponse> findAll() {
        return memberRepository.findAll().stream().map(MemberResponse::from).toList();
    }

    @Override
    public MemberResponse findById(Long id) {
        return memberRepository.findById(id)
                .map(MemberResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Member", id));
    }

    @Override
    @Transactional
    public MemberResponse create(MemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered: " + request.email());
        }
        if (memberRepository.existsByLibraryCardNo(request.libraryCardNo())) {
            throw new BusinessException("Library card already exists: " + request.libraryCardNo());
        }
        Member member = new Member();
        member.setLibraryCardNo(request.libraryCardNo());
        member.setFullName(request.fullName());
        member.setEmail(request.email());
        member.setPhone(request.phone());
        member.setJoinedAt(LocalDate.now());
        return MemberResponse.from(memberRepository.save(member));
    }
}
