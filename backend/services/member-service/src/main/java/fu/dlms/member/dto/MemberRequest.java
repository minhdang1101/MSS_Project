package fu.dlms.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Used to create and to edit a member. Every field is required. Name and email are capped at 100 characters.
 * Values are normalised on construction (trimmed; spaces, dots, dashes and brackets removed from the phone) so
 * that "0912 345 678" and "0912345678" are the same number for the uniqueness checks. The card number is never
 * supplied by the client: it is issued automatically and never changes.
 */
public record MemberRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 100, message = "Tối đa 100 ký tự") String fullName,
        @NotBlank(message = "Không được để trống") @Email(message = "Email không hợp lệ")
        @Size(max = 100, message = "Tối đa 100 ký tự") String email,
        @NotBlank(message = "Không được để trống")
        @Pattern(regexp = "^(\\+?[0-9]{8,15})?$", message = "Số điện thoại không hợp lệ (8-15 chữ số)") String phone
) {

    public MemberRequest {
        fullName = fullName == null ? null : fullName.trim();
        email = email == null ? null : email.trim();
        phone = phone == null ? null : phone.replaceAll("[\\s.\\-()]", "");
    }
}
