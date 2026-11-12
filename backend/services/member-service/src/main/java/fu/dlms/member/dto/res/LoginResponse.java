package fu.dlms.member.dto.res;

import java.time.Instant;

/** {@code role} is LIBRARIAN or MEMBER and decides which screens the web app shows. */
public record LoginResponse(String token, String tokenType, Instant expiresAt, String role, String username,
                            String fullName) {

    public static LoginResponse bearer(String token, Instant expiresAt, String role, String username, String fullName) {
        return new LoginResponse(token, "Bearer", expiresAt, role, username, fullName);
    }
}
