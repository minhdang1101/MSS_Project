package fu.dlms.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Tells the services who is calling, from the verified token and nothing else. Any X-Member-Id, X-Username or
 * X-User-Role header the browser sent is removed first, so these headers cannot be forged through the gateway.
 * The services behind it must not be reachable except through the gateway.
 */
@Component
public class IdentityHeadersFilter implements GlobalFilter, Ordered {

    static final String MEMBER_ID = "X-Member-Id";
    static final String USERNAME = "X-Username";
    static final String ROLE = "X-User-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast)
                .map(java.util.Optional::of)
                .defaultIfEmpty(java.util.Optional.empty())
                .flatMap(jwt -> chain.filter(exchange.mutate().request(withIdentity(exchange.getRequest(), jwt.orElse(null))).build()));
    }

    static ServerHttpRequest withIdentity(ServerHttpRequest request, Jwt jwt) {
        return request.mutate().headers(headers -> {
            headers.remove(MEMBER_ID);
            headers.remove(USERNAME);
            headers.remove(ROLE);
            if (jwt == null) {
                return;
            }
            headers.set(USERNAME, jwt.getSubject());
            String role = jwt.getClaimAsString("role");
            if (role != null) {
                headers.set(ROLE, role);
            }
            Object memberId = jwt.getClaim("mid");
            if (memberId != null) {
                headers.set(MEMBER_ID, String.valueOf(memberId));
            }
        }).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
