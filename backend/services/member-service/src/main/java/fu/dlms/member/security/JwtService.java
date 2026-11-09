package fu.dlms.member.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import fu.dlms.member.entity.Member;
import fu.dlms.member.entity.MemberRole;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Signs HS256 JWTs. The API gateway verifies them with the same shared secret. */
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;
    /** The value shipped in application.properties.example; publicly known, so never acceptable. */
    static final String EXAMPLE_SECRET = "your_jwt_secret_at_least_32_characters";

    private final JwtEncoder encoder;
    private final Duration lifetime;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        byte[] secret = properties.secret() == null ? new byte[0] : properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("app.security.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        if (EXAMPLE_SECRET.equals(properties.secret())) {
            throw new IllegalArgumentException(
                    "app.security.jwt.secret is still the placeholder from application.properties.example: "
                            + "anyone could forge tokens. Set your own secret (same value as the api-gateway).");
        }
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(secret, "HmacSHA256")));
        this.lifetime = Duration.ofMinutes(properties.expirationMinutes());
        this.clock = clock;
    }

    /**
     * A token for a librarian or a reader: subject = card number (or staff code), role = LIBRARIAN or MEMBER. A
     * reader's member id ("mid") is what the gateway forwards to the services so a reader can only ever be
     * answered with their own data.
     */
    public IssuedToken issue(Member account) {
        boolean reader = account.getRole() == MemberRole.MEMBER;
        return issue(account.getLibraryCardNo(), account.getFullName(), account.getRole().name(), reader ? account.getId() : null);
    }

    private IssuedToken issue(String subject, String name, String role, Long memberId) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(lifetime);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(subject)
                .claim("name", name)
                .claim("role", role)
                .issuedAt(now)
                .expiresAt(expiresAt);
        if (memberId != null) {
            claims.claim("mid", memberId);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
