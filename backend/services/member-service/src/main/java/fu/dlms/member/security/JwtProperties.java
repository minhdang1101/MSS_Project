package fu.dlms.member.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from app.security.jwt.*; the secret must be identical to the API gateway's. */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
