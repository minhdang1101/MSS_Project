package fu.dlms.member.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import fu.dlms.member.entity.Member;
import fu.dlms.member.entity.MemberStatus;

import java.time.LocalDate;

/**
 * {@code initialPassword} is filled in only in the answer to issuing a card or resetting a password, and is the one
 * and only time the plain password exists: it is omitted everywhere else and never stored (only its hash is).
 */
public record MemberResponse(
        Long id,
        String libraryCardNo,
        String fullName,
        String email,
        String phone,
        MemberStatus status,
        LocalDate joinedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) String initialPassword
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getLibraryCardNo(), member.getFullName(),
                member.getEmail(), member.getPhone(), member.getStatus(), member.getJoinedAt(), null);
    }

    public MemberResponse withInitialPassword(String password) {
        return new MemberResponse(id, libraryCardNo, fullName, email, phone, status, joinedAt, password);
    }
}
