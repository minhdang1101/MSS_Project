package fu.dlms.gateway.filter;

import fu.dlms.gateway.security.Roles;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Truyền danh tính người gọi xuống các service bằng header (Task G2 - tích hợp với Auth của Minh).
 *
 * <p>Các service phía sau không tự kiểm tra JWT (chỉ Gateway làm). Khi cần biết ai đang gọi, chúng đọc header:
 * <ul>
 *   <li>{@code X-Username}: claim "preferred_username" (mã thủ thư TT-xxxx hoặc số thẻ độc giả TV-xxxx)</li>
 *   <li>{@code X-User-Role}: role trong realm_access.roles (LIBRARIAN / MEMBER)</li>
 *   <li>{@code X-Member-Id}: claim "mid", chỉ có với độc giả</li>
 * </ul>
 * Vd notification-service GET
 * /api/notifications/me đọc X-Member-Id.
 *
 * <p><b>Chống giả mạo:</b> header do client tự gửi lên luôn bị XÓA trước, rồi mới điền lại từ token đã xác minh.
 * Nếu không xóa, ai cũng có thể gửi "X-Member-Id: 5" để xem thông báo của người khác.
 *
 * <p>GlobalFilter chạy cho mọi route, sau khi Spring Security đã xác minh token (Security là WebFilter, chạy trước).
 */
@Component
public class UserIdentityHeadersFilter implements GlobalFilter, Ordered {

    public static final String USERNAME = "X-Username";
    public static final String ROLE = "X-User-Role";
    public static final String MEMBER_ID = "X-Member-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(principal -> ((JwtAuthenticationToken) principal).getToken())
                .map(jwt -> withIdentity(exchange, jwt))
                // Không có token (route công khai): vẫn phải xóa header giả mạo
                .switchIfEmpty(Mono.fromSupplier(() -> withIdentity(exchange, null)))
                .flatMap(chain::filter);
    }

    static ServerWebExchange withIdentity(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(USERNAME);
            headers.remove(ROLE);
            headers.remove(MEMBER_ID);
            if (jwt != null) {
                setIfPresent(headers, USERNAME, jwt.getClaimAsString(Roles.USERNAME_CLAIM));
                setIfPresent(headers, ROLE, Roles.primaryRole(jwt));
                setIfPresent(headers, MEMBER_ID, jwt.getClaimAsString(Roles.MEMBER_ID_CLAIM));
            }
        }).build();
        return exchange.mutate().request(request).build();
    }

    private static void setIfPresent(org.springframework.http.HttpHeaders headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    /** Chạy sớm, trước các filter chuyển tiếp request của Gateway. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
