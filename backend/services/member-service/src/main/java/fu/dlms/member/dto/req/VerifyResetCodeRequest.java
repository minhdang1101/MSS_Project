package fu.dlms.member.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Step 2 of "forgot password": is the code from the e-mail right? Nothing is changed or used up. */
public record VerifyResetCodeRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 100, message = "Tối đa 100 ký tự") String email,
        @NotBlank(message = "Không được để trống") @Pattern(regexp = "[0-9]{6}", message = "Mã gồm 6 chữ số") String code
) {
}
