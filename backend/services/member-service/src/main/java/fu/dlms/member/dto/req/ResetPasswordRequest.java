package fu.dlms.member.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 100, message = "Tối đa 100 ký tự") String email,
        @NotBlank(message = "Không được để trống") @Pattern(regexp = "[0-9]{6}", message = "Mã gồm 6 chữ số") String code,
        @NotBlank(message = "Không được để trống")
        @Size(min = 8, max = 72, message = "Từ 8 đến 72 ký tự") String newPassword
) {
}
