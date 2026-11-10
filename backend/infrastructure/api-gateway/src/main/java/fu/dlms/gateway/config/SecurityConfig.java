package fu.dlms.gateway.config;

import fu.dlms.gateway.security.JsonSecurityErrorHandler;
import fu.dlms.gateway.security.Roles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;

import static fu.dlms.gateway.security.Roles.LIBRARIAN;
import static fu.dlms.gateway.security.Roles.MEMBER;

/**
 * Bảo mật ở API Gateway (Task G2, G3): xác minh JWT một lần duy nhất ở đây, rồi kiểm tra role theo bảng quyền.
 *
 * <p>Luồng của một request:
 * <ol>
 *   <li>CORS: cho phép frontend ở app.cors.allowed-origins gọi sang.</li>
 *   <li>Đọc header {@code Authorization: Bearer <token>}; {@link #jwtDecoder} kiểm tra chữ ký RS256 bằng khóa công khai
 *       (JWKS) của Keycloak, hạn dùng (exp) và nơi cấp (iss). Sai hoặc hết hạn → 401.</li>
 *   <li>{@link #roleConverter()} đọc role trong claim realm_access.roles thành quyền ROLE_LIBRARIAN / ROLE_MEMBER.</li>
 *   <li>So với bảng quyền trong {@link #securityWebFilterChain}. Không đủ quyền → 403.</li>
 *   <li>Qua hết thì UserIdentityHeadersFilter gắn X-Username / X-User-Role / X-Member-Id rồi chuyển tới service.</li>
 * </ol>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /** Defaults to true: forgetting the property must not leave every endpoint open. */
    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    /** Nơi Gateway tải khóa công khai của Keycloak (qua mạng nội bộ, có thể khác địa chỉ mà trình duyệt dùng). */
    @Value("${app.security.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /** Giá trị claim "iss" mà Keycloak ghi vào token (địa chỉ trình duyệt dùng, vd http://localhost:8180/realms/library). */
    @Value("${app.security.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JsonSecurityErrorHandler errors,
                                                         ReactiveJwtDecoder jwtDecoder) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        if (!securityEnabled) {
            return http.authorizeExchange(ex -> ex.anyExchange().permitAll()).build();
        }

        return http
                // Thứ tự quan trọng: Spring dùng quy tắc ĐẦU TIÊN khớp với request
                .authorizeExchange(ex -> ex
                        // ---- Công khai: không cần token ----
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()                 // CORS preflight
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // Đăng nhập, quên mật khẩu do Keycloak làm; chỉ còn đăng ký (cấp số thẻ + tạo tài khoản Keycloak) ở member-service
                        .pathMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/books/**").permitAll()   // tra cứu sách, ảnh bìa
                        // ---- Chỉ độc giả: dữ liệu của chính mình (memberId lấy từ token) ----
                        .pathMatchers("/api/me/**").hasRole(MEMBER)                   // phiếu mượn, tiền phạt của mình
                        .pathMatchers(HttpMethod.GET, "/api/notifications/me").hasRole(MEMBER)
                        .pathMatchers(HttpMethod.PATCH, "/api/notifications/*/read").hasRole(MEMBER)
                        // ---- Còn lại: chỉ thủ thư (quản lý sách, độc giả, mượn trả, phạt, thông báo...) ----
                        .pathMatchers("/api/**").hasRole(LIBRARIAN)
                        .anyExchange().hasRole(LIBRARIAN))                           // vd /actuator/gateway
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder).jwtAuthenticationConverter(roleConverter()))
                        .authenticationEntryPoint(errors)   // 401 dạng JSON
                        .accessDeniedHandler(errors))       // 403 dạng JSON
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .build();
    }

    /**
     * Keycloak để role trong claim lồng nhau realm_access.roles (không phải "scope" như Spring mong đợi), nên cần đọc
     * thủ công: role "LIBRARIAN" thành quyền "ROLE_LIBRARIAN" để hasRole("LIBRARIAN") khớp. Các role khác của Keycloak
     * (offline_access, uma_authorization...) bị bỏ qua.
     */
    static Converter<Jwt, Mono<AbstractAuthenticationToken>> roleConverter() {
        return jwt -> Mono.just(new JwtAuthenticationToken(jwt,
                Roles.appRoles(jwt).stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList(),
                jwt.getClaimAsString(Roles.USERNAME_CLAIM) != null ? jwt.getClaimAsString(Roles.USERNAME_CLAIM) : jwt.getSubject()));
    }

    /**
     * Kiểm tra chữ ký RS256 bằng khóa công khai tải từ Keycloak (JWKS, tải khi có request đầu tiên nên Gateway vẫn
     * khởi động được khi Keycloak chưa chạy), kiểm tra exp và iss. Không giữ secret nào: ai cũng có thể kiểm tra
     * token nhưng chỉ Keycloak ký được.
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuerUri);
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
