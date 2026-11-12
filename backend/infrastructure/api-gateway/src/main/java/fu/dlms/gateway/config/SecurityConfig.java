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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static fu.dlms.gateway.security.Roles.LIBRARIAN;
import static fu.dlms.gateway.security.Roles.MEMBER;

/**
 * Bảo mật ở API Gateway (Task G2, G3): xác minh JWT một lần duy nhất ở đây, rồi kiểm tra role theo bảng quyền.
 *
 * <p>Luồng của một request:
 * <ol>
 *   <li>CORS: cho phép frontend ở app.cors.allowed-origins gọi sang.</li>
 *   <li>Đọc header {@code Authorization: Bearer <token>}; {@link #jwtDecoder()} kiểm tra chữ ký HS256 (cùng secret với
 *       member-service) và hạn dùng (exp). Sai hoặc hết hạn → 401.</li>
 *   <li>{@link #roleConverter()} đọc claim "role" thành quyền ROLE_LIBRARIAN / ROLE_MEMBER.</li>
 *   <li>So với bảng quyền trong {@link #securityWebFilterChain}. Không đủ quyền → 403.</li>
 *   <li>Qua hết thì UserIdentityHeadersFilter gắn X-Username / X-User-Role / X-Member-Id rồi chuyển tới service.</li>
 * </ol>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final int MIN_SECRET_BYTES = 32;
    /** The value shipped in application.properties.example; publicly known, so never acceptable. */
    private static final String EXAMPLE_SECRET = "your_jwt_secret_at_least_32_characters";

    /** Defaults to true: forgetting the property must not leave every endpoint open. */
    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JsonSecurityErrorHandler errors) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http
                // Thứ tự quan trọng: Spring dùng quy tắc ĐẦU TIÊN khớp với request
                .authorizeExchange(ex -> ex
                        // ---- Công khai: không cần token ----
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()                 // CORS preflight
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register",
                                "/api/auth/forgot-password", "/api/auth/verify-reset-code",
                                "/api/auth/reset-password").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/books/**").permitAll()   // tra cứu sách, ảnh bìa
                        // ---- Mọi người đã đăng nhập ----
                        .pathMatchers(HttpMethod.POST, "/api/auth/password").hasAnyRole(LIBRARIAN, MEMBER)
                        // ---- Chỉ độc giả: dữ liệu của chính mình (memberId lấy từ token) ----
                        .pathMatchers("/api/me/**").hasRole(MEMBER)                   // phiếu mượn, tiền phạt của mình
                        .pathMatchers(HttpMethod.GET, "/api/notifications/me").hasRole(MEMBER)
                        .pathMatchers(HttpMethod.PATCH, "/api/notifications/*/read").hasRole(MEMBER)
                        // ---- Còn lại: chỉ thủ thư (quản lý sách, độc giả, mượn trả, phạt, thông báo...) ----
                        .pathMatchers("/api/**").hasRole(LIBRARIAN)
                        .anyExchange().hasRole(LIBRARIAN))                           // vd /actuator/gateway
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()).jwtAuthenticationConverter(roleConverter()))
                        .authenticationEntryPoint(errors)   // 401 dạng JSON
                        .accessDeniedHandler(errors))       // 403 dạng JSON
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .build();
    }

    /**
     * Đọc claim "role" (vd "LIBRARIAN") thành quyền "ROLE_LIBRARIAN"; hasRole("LIBRARIAN") so với chính chuỗi này.
     * Mặc định Spring chỉ đọc claim "scope"/"scp", nên thiếu bước này thì mọi kiểm tra role đều thất bại.
     */
    static Converter<Jwt, Mono<AbstractAuthenticationToken>> roleConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(Roles.ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    /**
     * Kiểm tra chữ ký HS256 bằng secret dùng chung với member-service, và kiểm tra exp (hết hạn) mặc định.
     * Từ chối khởi động nếu secret quá ngắn hoặc vẫn là giá trị mẫu (ai cũng biết → ai cũng giả được token).
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.security.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        if (EXAMPLE_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("app.security.jwt.secret is still the placeholder from "
                    + "application.properties.example: anyone could forge tokens. Set your own secret "
                    + "(same value as member-service).");
        }
        SecretKeySpec key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
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
