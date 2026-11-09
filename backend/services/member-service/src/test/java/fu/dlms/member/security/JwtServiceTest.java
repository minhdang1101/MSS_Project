package fu.dlms.member.security;

import fu.dlms.member.entity.Member;
import fu.dlms.member.entity.MemberRole;
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
    /** The decoder checks expiry against the real clock, so the fixed test clock must start at the real "now". */
    private static final Instant NOW = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private static Member librarian() {
        Member l = new Member();
        l.setRole(MemberRole.LIBRARIAN);
        l.setId(1L);
        l.setLibraryCardNo("TT-0001");
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
    void aReaderTokenCarriesTheMemberRoleAndTheMemberIdThatTheGatewayForwards() {
        fu.dlms.member.entity.Member member = new fu.dlms.member.entity.Member();
        member.setId(42L);
        member.setLibraryCardNo("TV-0412");
        member.setFullName("Phạm Minh Khôi");
        JwtService service = new JwtService(new JwtProperties(SECRET, 480), clock);

        Jwt jwt = decoder(SECRET).decode(service.issue(member).token());

        assertThat(jwt.getSubject()).isEqualTo("TV-0412");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("MEMBER");
        assertThat(jwt.<Long>getClaim("mid")).isEqualTo(42L);
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Phạm Minh Khôi");
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(480)));
    }

    @Test
    void aLibrarianTokenHasNoMemberId() {
        JwtService service = new JwtService(new JwtProperties(SECRET, 480), clock);

        Jwt jwt = decoder(SECRET).decode(service.issue(librarian()).token());

        assertThat(jwt.hasClaim("mid")).isFalse();
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
