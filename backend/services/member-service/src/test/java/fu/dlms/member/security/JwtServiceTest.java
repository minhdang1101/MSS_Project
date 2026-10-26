package fu.dlms.member.security;

import fu.dlms.member.entity.Librarian;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit_test_secret_that_is_at_least_32_chars";
    private static final Instant NOW = Instant.parse("2026-10-26T08:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private static Librarian librarian() {
        Librarian l = new Librarian();
        l.setStaffCode("TT-0001");
        l.setFullName("Nguyễn Thu Trang");
        return l;
    }

    private static JwtDecoder decoder(String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test
    void issuedTokenCarriesIdentityAndExpiry() {
        JwtService service = new JwtService(new JwtProperties(SECRET, 480), clock);

        IssuedToken issued = service.issue(librarian());

        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(480)));
        Jwt jwt = decoder(SECRET).decode(issued.token());
        assertThat(jwt.getSubject()).isEqualTo("TT-0001");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Nguyễn Thu Trang");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("LIBRARIAN");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(480)));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejectedByVerifier() {
        JwtService service = new JwtService(new JwtProperties(SECRET, 480), clock);
        IssuedToken issued = service.issue(librarian());

        assertThatThrownBy(() -> decoder("a_completely_different_secret_32_chars!!").decode(issued.token()))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    void thePublicPlaceholderSecretFromTheExampleFileIsRejectedAtStartup() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("your_jwt_secret_at_least_32_characters", 480), clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("placeholder");
    }

    @Test
    void secretShorterThan32BytesIsRejectedAtStartup() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too_short", 480), clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
    }
}
