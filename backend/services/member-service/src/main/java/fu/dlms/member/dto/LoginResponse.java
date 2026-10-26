package fu.dlms.member.dto;

import java.time.Instant;

public record LoginResponse(String token, String tokenType, Instant expiresAt, String staffCode, String fullName) {

    public static LoginResponse bearer(String token, Instant expiresAt, String staffCode, String fullName) {
        return new LoginResponse(token, "Bearer", expiresAt, staffCode, fullName);
    }
}
