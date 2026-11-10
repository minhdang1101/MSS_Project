package fu.dlms.member.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The only place that talks to Keycloak's Admin API. Passwords, sign-in, the forgot-password e-mail and tokens all
 * live in Keycloak; member-service keeps the reader's profile (card number, contact details, status) and uses this
 * client to create the matching login account, set a password, and switch the account off when a card is locked.
 *
 * <p>It signs in to Keycloak as its own client ("member-service", client credentials), whose service account may
 * only manage users. The username of a reader's account is the library card number (TV-0412).
 */
@Component
public class KeycloakAdminClient {

    static final String MEMBER_ROLE = "MEMBER";
    private static final Duration TOKEN_MARGIN = Duration.ofSeconds(20);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() { };

    private final RestClient http;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final Clock clock;

    private String accessToken;
    private Instant accessTokenExpiresAt = Instant.EPOCH;

    public KeycloakAdminClient(@Value("${app.keycloak.base-url}") String baseUrl,
                               @Value("${app.keycloak.realm:library}") String realm,
                               @Value("${app.keycloak.client-id:member-service}") String clientId,
                               @Value("${app.keycloak.client-secret}") String clientSecret,
                               Clock clock) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clock = clock;
    }

    /**
     * Creates the login account of a reader: username = card number, role MEMBER, and the attribute "mid" (the id of
     * the member row) which Keycloak puts into the token so the gateway knows whose data to show.
     *
     * @param temporaryPassword true when the reader must choose a new password at the first login (a password the
     *                          library handed over); false for a password the reader chose themselves
     * @throws KeycloakException when Keycloak is unreachable, or the account already exists
     */
    public void createReader(String cardNo, String email, String fullName, long memberId, String password,
                             boolean temporaryPassword) {
        Map<String, Object> user = Map.of(
                "username", cardNo,
                "email", email,
                "firstName", fullName,
                "lastName", "",
                "enabled", true,
                "emailVerified", true,
                "attributes", Map.of("mid", List.of(String.valueOf(memberId))),
                "credentials", List.of(credential(password, temporaryPassword)));
        URI location = call("tạo tài khoản", () -> http.post()
                .uri("/admin/realms/{realm}/users", realm)
                .headers(h -> h.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(user)
                .retrieve()
                .onStatus(status -> status.value() == 409, (req, res) -> {
                    throw new KeycloakException("Tài khoản đăng nhập đã tồn tại trong Keycloak (số thẻ hoặc email trùng).");
                })
                .toBodilessEntity()
                .getHeaders().getLocation());
        String userId = location == null ? findUserId(cardNo) : lastSegment(location);
        try {
            assignMemberRole(userId);
        } catch (KeycloakException e) {
            // An account without its role is useless and would block the same card number next time: take it back
            try {
                deleteUser(userId);
            } catch (KeycloakException ignored) {
                // the original error is the one worth reporting
            }
            throw e;
        }
    }

    /** Sets a new password. With {@code temporary} the reader must change it at the next login. */
    public void setPassword(String cardNo, String password, boolean temporary) {
        String userId = findUserId(cardNo);
        call("đặt mật khẩu", () -> http.put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", realm, userId)
                .headers(h -> h.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(credential(password, temporary))
                .retrieve().toBodilessEntity());
    }

    /** A LOCKED card cannot sign in any more: the account is switched off (and back on when the card is reopened). */
    public void setEnabled(String cardNo, boolean enabled) {
        String userId = findUserId(cardNo);
        call("đổi trạng thái tài khoản", () -> http.put()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .headers(h -> h.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("enabled", enabled))
                .retrieve().toBodilessEntity());
    }

    /** Keeps the e-mail and the name Keycloak shows (and puts in the token) in step with the member's profile. */
    public void updateProfile(String cardNo, String email, String fullName) {
        String userId = findUserId(cardNo);
        call("cập nhật tài khoản", () -> http.put()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .headers(h -> h.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "firstName", fullName, "lastName", ""))
                .retrieve().toBodilessEntity());
    }

    /** Removes an account; used to undo {@link #createReader} when the member row could not be saved. */
    public void deleteReader(String cardNo) {
        deleteUser(findUserId(cardNo));
    }

    private void deleteUser(String userId) {
        call("xóa tài khoản", () -> http.delete()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .headers(h -> h.setBearerAuth(token()))
                .retrieve().toBodilessEntity());
    }

    // ---------------------------------------------------------------------------------------------

    private void assignMemberRole(String userId) {
        Map<String, Object> role = call("đọc role MEMBER", () -> http.get()
                .uri("/admin/realms/{realm}/roles/{name}", realm, MEMBER_ROLE)
                .headers(h -> h.setBearerAuth(token()))
                .retrieve().body(MAP));
        call("gán role MEMBER", () -> http.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                .headers(h -> h.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve().toBodilessEntity());
    }

    private String findUserId(String cardNo) {
        List<Map<String, Object>> found = call("tìm tài khoản", () -> http.get()
                .uri(uri -> uri.path("/admin/realms/{realm}/users")
                        .queryParam("username", cardNo).queryParam("exact", true).build(realm))
                .headers(h -> h.setBearerAuth(token()))
                .retrieve().body(LIST));
        if (found == null || found.isEmpty()) {
            throw new KeycloakException("Không tìm thấy tài khoản đăng nhập của thẻ " + cardNo + " trong Keycloak.");
        }
        return String.valueOf(found.get(0).get("id"));
    }

    private static Map<String, Object> credential(String password, boolean temporary) {
        return Map.of("type", "password", "value", password, "temporary", temporary);
    }

    private static String lastSegment(URI location) {
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** A token for this service's own account, reused until shortly before it expires. */
    private synchronized String token() {
        Instant now = clock.instant();
        if (accessToken != null && now.isBefore(accessTokenExpiresAt.minus(TOKEN_MARGIN))) {
            return accessToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        Map<String, Object> answer = call("đăng nhập vào Keycloak", () -> http.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve().body(MAP));
        if (answer == null || answer.get("access_token") == null) {
            throw new KeycloakException("Keycloak không cấp token cho member-service. Kiểm tra app.keycloak.client-secret.");
        }
        accessToken = String.valueOf(answer.get("access_token"));
        long seconds = answer.get("expires_in") instanceof Number n ? n.longValue() : 60;
        accessTokenExpiresAt = now.plusSeconds(seconds);
        return accessToken;
    }

    /** Runs a Keycloak call and turns any failure into one {@link KeycloakException} with a readable message. */
    private <T> T call(String what, Supplier<T> action) {
        try {
            return action.get();
        } catch (KeycloakException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new KeycloakException("Không " + what + " được ở Keycloak (HTTP " + e.getStatusCode().value() + ").", e);
        } catch (RestClientException e) {
            throw new KeycloakException("Không " + what + " được: không kết nối được Keycloak, kiểm tra Keycloak đã chạy chưa.", e);
        }
    }
}
