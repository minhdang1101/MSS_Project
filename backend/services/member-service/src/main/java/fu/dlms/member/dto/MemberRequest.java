package fu.dlms.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberRequest(
        @NotBlank @Size(max = 30) String libraryCardNo,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email String email,
        @Size(max = 20) String phone
) {
}
