package fu.dlms.member.keycloak;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The Admin API calls, against a small fake Keycloak that records what it was asked. */
class KeycloakAdminClientTest {

    private record Call(String method, String path, String query, String auth, String body) {
    }

    private HttpServer server;
    private final List<Call> calls = new ArrayList<>();
    private int createStatus = 201;
    private int roleStatus = 200;
    private KeycloakAdminClient client;
    private Instant now = Instant.parse("2026-11-10T10:00:00Z");

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String path = exchange.getRequestURI().getPath();
            calls.add(new Call(exchange.getRequestMethod(), path, exchange.getRequestURI().getQuery(),
                    exchange.getRequestHeaders().getFirst("Authorization"), body));
            String answer = "";
            int status = 204;
            if (path.endsWith("/protocol/openid-connect/token")) {
                answer = "{\"access_token\":\"service-token\",\"expires_in\":300}";
                status = 200;
            } else if (path.equals("/admin/realms/library/users") && exchange.getRequestMethod().equals("POST")) {
                status = createStatus;
                if (status == 201) {
                    exchange.getResponseHeaders().add("Location", "http://localhost/admin/realms/library/users/u-1");
                }
            } else if (path.equals("/admin/realms/library/users") && exchange.getRequestMethod().equals("GET")) {
                answer = exchange.getRequestURI().getQuery().contains("TV-404") ? "[]" : "[{\"id\":\"u-1\",\"username\":\"TV-1\"}]";
                status = 200;
            } else if (path.equals("/admin/realms/library/roles/MEMBER")) {
                answer = roleStatus == 200 ? "{\"id\":\"role-1\",\"name\":\"MEMBER\"}" : "";
                status = roleStatus;
            }
            byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
            if (status != 204) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();
        Clock clock = new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now;
            }
        };
        client = new KeycloakAdminClient("http://localhost:" + server.getAddress().getPort(), "library", "member-service",
                "the-secret", clock);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private List<Call> to(String method, String pathEnd) {
        return calls.stream().filter(c -> c.method().equals(method) && c.path().endsWith(pathEnd)).toList();
    }

    @Test
    void createsTheReaderAccountWithCardNumberAttributeAndPasswordThenGivesItTheMemberRole() {
        client.createReader("TV-0412", "an@example.com", "Lê An", 42L, "Temp-pass-1", true);

        Call create = to("POST", "/admin/realms/library/users").get(0);
        assertThat(create.auth()).isEqualTo("Bearer service-token");
        assertThat(create.body()).contains("\"username\":\"TV-0412\"", "\"email\":\"an@example.com\"", "\"firstName\":\"Lê An\"")
                .contains("\"mid\":[\"42\"]").contains("\"temporary\":true").contains("\"value\":\"Temp-pass-1\"");
        assertThat(to("GET", "/roles/MEMBER")).hasSize(1);
        Call role = to("POST", "/users/u-1/role-mappings/realm").get(0);
        assertThat(role.body()).contains("\"name\":\"MEMBER\"");
    }

    @Test
    void anAccountThatCouldNotGetItsRoleIsTakenBackSoItDoesNotBlockTheCardNumber() {
        roleStatus = 403;

        assertThatThrownBy(() -> client.createReader("TV-0412", "an@example.com", "Lê An", 42L, "Temp-pass-1", true))
                .isInstanceOf(KeycloakException.class).hasMessageContaining("MEMBER");

        assertThat(to("DELETE", "/admin/realms/library/users/u-1")).hasSize(1);
    }

    @Test
    void aPasswordTheReaderChoseIsNotTemporary() {
        client.createReader("TV-0412", "an@example.com", "Lê An", 42L, "my-own-pass", false);

        assertThat(to("POST", "/admin/realms/library/users").get(0).body()).contains("\"temporary\":false");
    }

    @Test
    void signsInWithTheClientSecretAndReusesTheTokenUntilItIsAboutToExpire() {
        client.setEnabled("TV-1", false);
        client.setEnabled("TV-1", true);

        List<Call> tokenCalls = to("POST", "/protocol/openid-connect/token");
        assertThat(tokenCalls).hasSize(1);
        assertThat(tokenCalls.get(0).body()).contains("grant_type=client_credentials", "client_id=member-service", "client_secret=the-secret");

        now = now.plusSeconds(290); // inside the 20 second margin before the 300 second expiry
        client.setEnabled("TV-1", true);
        assertThat(to("POST", "/protocol/openid-connect/token")).hasSize(2);
    }

    @Test
    void switchesAnAccountOffAndOn() {
        client.setEnabled("TV-1", false);

        Call put = to("PUT", "/admin/realms/library/users/u-1").get(0);
        assertThat(put.body()).isEqualTo("{\"enabled\":false}");
    }

    @Test
    void setsAPasswordThroughTheResetPasswordEndpoint() {
        client.setPassword("TV-1", "New-temp-2", true);

        Call put = to("PUT", "/admin/realms/library/users/u-1/reset-password").get(0);
        assertThat(put.body()).contains("\"value\":\"New-temp-2\"").contains("\"temporary\":true").contains("\"type\":\"password\"");
    }

    @Test
    void keepsTheNameAndEmailInStep() {
        client.updateProfile("TV-1", "new@example.com", "Tên mới");

        assertThat(to("PUT", "/admin/realms/library/users/u-1").get(0).body())
                .contains("\"email\":\"new@example.com\"", "\"firstName\":\"Tên mới\"");
    }

    @Test
    void anAccountThatIsNotInKeycloakIsReported() {
        assertThatThrownBy(() -> client.setEnabled("TV-404", false))
                .isInstanceOf(KeycloakException.class).hasMessageContaining("TV-404");
    }

    @Test
    void aDuplicateAccountIsReportedInPlainWords() {
        createStatus = 409;

        assertThatThrownBy(() -> client.createReader("TV-1", "an@example.com", "An", 1L, "Temp-pass-1", true))
                .isInstanceOf(KeycloakException.class).hasMessageContaining("đã tồn tại");
    }

    @Test
    void keycloakBeingDownIsReportedInsteadOfLeakingATechnicalError() {
        server.stop(0);

        assertThatThrownBy(() -> client.setEnabled("TV-1", false))
                .isInstanceOf(KeycloakException.class).hasMessageContaining("Keycloak");
    }
}
