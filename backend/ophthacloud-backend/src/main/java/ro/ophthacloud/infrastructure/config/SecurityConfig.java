package ro.ophthacloud.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ro.ophthacloud.infrastructure.web.TenantResolutionFilter;
import ro.ophthacloud.shared.security.OphthaClinicalJwtConverter;

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
 * The {@link OphthaClinicalJwtConverter} extracts {@link ro.ophthacloud.shared.security.OphthaPrincipal}
 * from each JWT and populates the {@link org.springframework.security.core.context.SecurityContextHolder}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final OphthaClinicalJwtConverter jwtConverter;
    private final TenantResolutionFilter tenantResolutionFilter;

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
                // ALL other requests: denied unless a @PreAuthorize opens them
                .anyRequest().denyAll()
            )

            // ── OAuth2 Resource Server: JWT via Keycloak ──────────────────────
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            )

            // ── Tenant resolution: runs after JWT auth populates SecurityContext ─
            .addFilterAfter(tenantResolutionFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
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
