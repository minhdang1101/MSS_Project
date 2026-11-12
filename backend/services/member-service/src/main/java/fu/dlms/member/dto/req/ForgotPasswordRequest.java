package fu.dlms.member.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Không được để trống") @Email(message = "Email không hợp lệ")
        @Size(max = 100, message = "Tối đa 100 ký tự") String email
) {
}
