package fu.dlms.member.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One login form for everyone: a librarian types the staff code (TT-0001), a reader types the library card number
 * (TV-0412). "staffCode" is still accepted as the field name so older clients keep working.
 */
public record LoginRequest(
        @JsonAlias("staffCode") @NotBlank(message = "Không được để trống") @Size(max = 30, message = "Tối đa 30 ký tự") String username,
        @NotBlank(message = "Không được để trống") @Size(max = 72, message = "Tối đa 72 ký tự") String password
) {
}
