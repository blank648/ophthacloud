package ro.ophthacloud.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ro.ophthacloud.infrastructure.web.TenantResolutionFilter;
import ro.ophthacloud.shared.security.OphthaClinicalJwtConverter;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Security configuration for OphthaCloud backend.
 * <p>
 * Key policies per GUIDE_05 §4.1:
 * <ul>
 *   <li>Stateless — no server-side sessions, every request validated against Keycloak JWT</li>
 *   <li>CSRF disabled — SPA clients use Bearer tokens, not cookies</li>
 *   <li>All endpoints deny-all by default — only explicitly listed paths are permitted</li>
 *   <li>Method security enabled — {@code @PreAuthorize} annotations drive RBAC per endpoint</li>
 * </ul>
 *
 * <p><strong>Test profile support:</strong> When
 * {@code spring.security.oauth2.resourceserver.jwt.secret} is set (via
 * {@code application-test.yml}), an HMAC-based {@link JwtDecoder} is registered
 * instead of the Keycloak JWK-set-uri decoder. This allows tests to sign JWTs with a
 * fixed secret via {@code TestJwtFactory} without running Keycloak.
 *
 * The {@link OphthaClinicalJwtConverter} extracts {@link ro.ophthacloud.shared.security.OphthaPrincipal}
 * from each JWT and populates the {@link org.springframework.security.core.context.SecurityContextHolder}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final OphthaClinicalJwtConverter jwtConverter;
    private final TenantResolutionFilter tenantResolutionFilter;

    /**
     * Injected from {@code application-test.yml} when running under the {@code test} profile.
     * In production this property is absent (empty string) and Keycloak JWK-set-uri is used.
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.secret:}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── Session: stateless — no HttpSession ever created ───────────────
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── CSRF: disabled — SPA sends Bearer tokens via Authorization header
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS: frontend origins only ───────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Authorization rules ───────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Actuator health — no auth needed
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Swagger / OpenAPI — no auth needed in non-prod
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                 "/v3/api-docs/**", "/api-docs/**").permitAll()
                // Public API placeholder (future use)
                .requestMatchers("/api/v1/public/**").permitAll()
                // FHIR — authenticated but no module permission check
                .requestMatchers("/fhir/r4/**").authenticated()
                // ALL other requests: must be authenticated, then @PreAuthorize drives authorization
                .anyRequest().authenticated()
            )

            // ── OAuth2 Resource Server: JWT (Keycloak JWK or test HMAC secret) ─
            .oauth2ResourceServer(oauth2 -> {
                if (jwtSecret != null && !jwtSecret.isBlank()) {
                    // Test profile: validate tokens with a shared HMAC secret
                    log.info("SecurityConfig: using HMAC secret-based JWT decoder (test profile)");
                    oauth2.jwt(jwt -> jwt
                            .decoder(hmacJwtDecoder(jwtSecret))
                            .jwtAuthenticationConverter(jwtConverter));
                } else {
                    // Production: validate tokens against Keycloak JWK set URI
                    oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter));
                }
            })

            // ── Tenant resolution: runs after JWT auth populates SecurityContext ─
            .addFilterAfter(tenantResolutionFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Builds an HMAC HS256 {@link JwtDecoder} for test-profile use.
     * Never called in production (secret is absent from {@code application.yml}).
     */
    private JwtDecoder hmacJwtDecoder(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * CORS configuration: allows the Next.js dev frontend and production domain.
     * All HTTP methods and headers are allowed; credentials are required (cookies / auth headers).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",          // Next.js dev
                "http://localhost:3001",          // Patient portal dev
                "https://app.ophthacloud.ro",     // Production frontend
                "https://portal.ophthacloud.ro"  // Production portal
        ));
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Request-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
