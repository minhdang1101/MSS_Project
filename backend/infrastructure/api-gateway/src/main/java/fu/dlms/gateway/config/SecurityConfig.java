package fu.dlms.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final int MIN_SECRET_BYTES = 32;
    private static final String LIBRARIAN_ROLE = "LIBRARIAN";
    private static final String MEMBER_ROLE = "MEMBER";
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
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        if (!securityEnabled) {
            return http.authorizeExchange(ex -> ex.anyExchange().permitAll()).build();
        }

        return http
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register",
                                "/api/auth/forgot-password", "/api/auth/verify-reset-code", "/api/auth/reset-password").permitAll()
                        .pathMatchers("/api/auth/password").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                        // Readers (role MEMBER) may only browse the catalogue above; every other API is for librarians.
                        // A reader's own data: only readers, and the services then read the member id the gateway sets.
                        .pathMatchers("/api/me/**").access(roleOnly(MEMBER_ROLE))
                        .anyExchange().access(roleOnly(LIBRARIAN_ROLE)))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtDecoder(jwtDecoder())))
                .build();
    }

    /** Allows the call only when the verified token's "role" claim equals {@code role} (member-service writes it). */
    private static ReactiveAuthorizationManager<AuthorizationContext> roleOnly(String role) {
        return (authentication, context) -> authentication
                .map(auth -> new AuthorizationDecision(
                        auth.getPrincipal() instanceof Jwt jwt && role.equals(jwt.getClaimAsString("role"))))
                .defaultIfEmpty(new AuthorizationDecision(false));
    }

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
