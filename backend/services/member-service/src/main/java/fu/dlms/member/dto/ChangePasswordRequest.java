package fu.dlms.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 72, message = "Tối đa 72 ký tự") String currentPassword,
        @NotBlank(message = "Không được để trống")
        @Size(min = 8, max = 72, message = "Từ 8 đến 72 ký tự") String newPassword
) {
}
