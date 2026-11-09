package fu.dlms.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A reader signing up on their own (no phone number: it can be added later by a librarian). The card number is issued automatically and returned in the login answer. */
public record RegisterRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 100, message = "Tối đa 100 ký tự") String fullName,
        @NotBlank(message = "Không được để trống") @Size(max = 100, message = "Tối đa 100 ký tự") String email,
        @NotBlank(message = "Không được để trống")
        @Size(min = 8, max = 72, message = "Từ 8 đến 72 ký tự") String password
) {
}
