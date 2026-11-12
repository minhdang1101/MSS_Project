package fu.dlms.gateway;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Kiểm thử bảo mật của API Gateway (Task G1-G3) với token thật ký bằng HS256.
 *
 * <p>Không cần Eureka hay các service thật: member-service và notification-service được thay bằng một HTTP server giả
 * (đăng ký qua spring.cloud.discovery.client.simple), server này trả lại các header nó nhận được để test kiểm tra
 * Gateway đã truyền danh tính đúng chưa. loan-service không có instance nào nên Gateway trả 503: điều đó chứng tỏ
 * request đã vượt qua lớp bảo mật.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "app.security.enabled=true",
        "app.security.jwt.secret=" + GatewaySecurityTest.SECRET,
        "app.cors.allowed-origins=http://localhost:5173"
})
class GatewaySecurityTest {

    static final String SECRET = "test-secret-for-gateway-security-tests-0123456789";

    /** Service giả: trả về JSON chứa các header danh tính nhận được. */
    private static final HttpServer STUB = startStub();

    @Autowired
    private WebTestClient client;

    @DynamicPropertySource
    static void stubServices(DynamicPropertyRegistry registry) {
        String uri = "http://localhost:" + STUB.getAddress().getPort();
        registry.add("spring.cloud.discovery.client.simple.instances.member-service[0].uri", () -> uri);
        registry.add("spring.cloud.discovery.client.simple.instances.notification-service[0].uri", () -> uri);
    }

    @AfterAll
    static void stopStub() {
        STUB.stop(0);
    }

    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                var h = exchange.getRequestHeaders();
                String body = "{\"username\":\"%s\",\"role\":\"%s\",\"memberId\":\"%s\"}".formatted(
                        Optional.ofNullable(h.getFirst("X-Username")).orElse(""),
                        Optional.ofNullable(h.getFirst("X-User-Role")).orElse(""),
                        Optional.ofNullable(h.getFirst("X-Member-Id")).orElse(""));
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ===================== Tạo token giống member-service =====================

    private static String token(String secret, String subject, String role, Long memberId, Instant expiresAt) {
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(expiresAt.minus(Duration.ofHours(8)))
                .expiresAt(expiresAt);
        if (memberId != null) {
            claims.claim("mid", memberId);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private static String librarian() {
        return token(SECRET, "TT-0001", "LIBRARIAN", null, Instant.now().plus(Duration.ofHours(1)));
    }

    private static String member() {
        return token(SECRET, "TV-0087", "MEMBER", 2L, Instant.now().plus(Duration.ofHours(1)));
    }

    // ===================== JWT verification =====================

    @Test
    @DisplayName("Không có token: 401, JSON báo chưa đăng nhập")
    void noTokenIs401() {
        client.get().uri("/api/members").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").value(m -> org.assertj.core.api.Assertions.assertThat((String) m).contains("chưa đăng nhập"))
                .jsonPath("$.path").isEqualTo("/api/members");
    }

    @Test
    @DisplayName("Token hết hạn: 401, JSON báo phiên đăng nhập đã hết hạn")
    void expiredTokenIs401() {
        String expired = token(SECRET, "TT-0001", "LIBRARIAN", null, Instant.now().minus(Duration.ofHours(1)));

        client.get().uri("/api/members").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired).exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.message").value(m ->
                        org.assertj.core.api.Assertions.assertThat((String) m).contains("hết hạn"));
    }

    @Test
    @DisplayName("Token ký bằng secret khác (giả mạo): 401 token không hợp lệ")
    void forgedTokenIs401() {
        String forged = token("another-secret-that-is-at-least-32-bytes-long", "TT-0001", "LIBRARIAN", null,
                Instant.now().plus(Duration.ofHours(1)));

        client.get().uri("/api/members").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged).exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.message").isEqualTo("Token không hợp lệ");
    }

    @Test
    @DisplayName("Token rác (không phải JWT): 401")
    void garbageTokenIs401() {
        client.get().uri("/api/members").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def").exchange()
                .expectStatus().isUnauthorized();
    }

    // ===================== Role checks =====================

    @Test
    @DisplayName("Thủ thư gọi API quản lý: được chuyển tới service, kèm X-Username và X-User-Role")
    void librarianReachesServiceWithIdentity() {
        client.get().uri("/api/members").header(HttpHeaders.AUTHORIZATION, "Bearer " + librarian()).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("TT-0001")
                .jsonPath("$.role").isEqualTo("LIBRARIAN")
                .jsonPath("$.memberId").isEqualTo("");
    }

    @Test
    @DisplayName("Độc giả gọi API của thủ thư: 403, JSON báo không có quyền")
    void memberCannotUseLibrarianApi() {
        client.get().uri("/api/members").header(HttpHeaders.AUTHORIZATION, "Bearer " + member()).exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("Bạn không có quyền thực hiện thao tác này");
    }

    @Test
    @DisplayName("Độc giả xem thông báo của mình: X-Member-Id lấy từ token, header giả mạo bị ghi đè")
    void memberSeesOwnNotificationsAndCannotSpoofId() {
        client.get().uri("/api/notifications/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + member())
                .header("X-Member-Id", "99")          // cố tình giả mạo
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.memberId").isEqualTo("2")
                .jsonPath("$.role").isEqualTo("MEMBER")
                .jsonPath("$.username").isEqualTo("TV-0087");
    }

    @Test
    @DisplayName("Độc giả đánh dấu đã đọc: được phép")
    void memberCanMarkRead() {
        client.patch().uri("/api/notifications/5/read").header(HttpHeaders.AUTHORIZATION, "Bearer " + member())
                .exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("Thủ thư gọi /api/notifications/me (dành cho độc giả): 403")
    void librarianCannotUseMemberOnlyApi() {
        client.get().uri("/api/notifications/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + librarian())
                .exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Cả hai role đều đổi được mật khẩu của chính mình")
    void bothRolesCanChangePassword() {
        client.post().uri("/api/auth/password").header(HttpHeaders.AUTHORIZATION, "Bearer " + member())
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.role").isEqualTo("MEMBER");
        client.post().uri("/api/auth/password").header(HttpHeaders.AUTHORIZATION, "Bearer " + librarian())
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.role").isEqualTo("LIBRARIAN");
    }

    // ===================== Public routes & routing =====================

    @Test
    @DisplayName("Đăng nhập không cần token, và header danh tính giả mạo bị xóa")
    void loginIsPublicAndSpoofedHeadersAreRemoved() {
        client.post().uri("/api/auth/login")
                .header("X-User-Role", "LIBRARIAN")    // cố tình giả mạo
                .header("X-Username", "TT-0001")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.role").isEqualTo("")
                .jsonPath("$.username").isEqualTo("");
    }

    @Test
    @DisplayName("Service chưa có instance trên Eureka: 503 (request đã qua lớp bảo mật)")
    void serviceWithoutInstanceIs503() {
        client.get().uri("/api/loans").header(HttpHeaders.AUTHORIZATION, "Bearer " + librarian()).exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    @DisplayName("Health check công khai để Docker kiểm tra Gateway còn sống")
    void healthIsPublic() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }
}
