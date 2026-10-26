package fu.dlms.member.dto;

import fu.dlms.member.entity.Member;
import fu.dlms.member.entity.MemberStatus;

import java.time.LocalDate;

public record MemberResponse(
        Long id,
        String libraryCardNo,
        String fullName,
        String email,
        String phone,
        MemberStatus status,
        LocalDate joinedAt
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getLibraryCardNo(), member.getFullName(),
                member.getEmail(), member.getPhone(), member.getStatus(), member.getJoinedAt());
    }
}
