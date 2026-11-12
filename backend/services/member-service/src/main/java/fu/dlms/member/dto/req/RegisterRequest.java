package fu.dlms.member.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A reader signing up on their own. Same rules for name, email and phone as the librarian's member form (the phone is
 * cleaned of spaces, dots, dashes and brackets first, so "0912 345 678" and "0912345678" are the same number). The card
 * number is issued automatically and returned in the login answer.
 */
public record RegisterRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 100, message = "Tối đa 100 ký tự") String fullName,
        @NotBlank(message = "Không được để trống") @Size(max = 100, message = "Tối đa 100 ký tự") String email,
        @NotBlank(message = "Không được để trống")
        @Pattern(regexp = "^(\\+?[0-9]{8,15})?$", message = "Số điện thoại không hợp lệ (8-15 chữ số)") String phone,
        @NotBlank(message = "Không được để trống")
        @Size(min = 8, max = 72, message = "Từ 8 đến 72 ký tự") String password
) {

    public RegisterRequest {
        phone = phone == null ? null : phone.replaceAll("[\\s.\\-()]", "");
    }
}
