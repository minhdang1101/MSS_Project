package fu.dlms.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 30, message = "Tối đa 30 ký tự") String staffCode,
        @NotBlank(message = "Không được để trống") @Size(max = 72, message = "Tối đa 72 ký tự") String password
) {
}
