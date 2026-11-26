package ro.unibuc.fmi.ophthacloud.securityaudit.config;

import java.util.List;
import ro.unibuc.fmi.ophthacloud.securityaudit.repository.UserRepository;
import ro.unibuc.fmi.ophthacloud.securityaudit.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenService tokens, UserRepository users,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver errors) throws Exception {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("http://localhost:5173"));
        cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        var origins = new UrlBasedCorsConfigurationSource();
        origins.registerCorsConfiguration("/api/**", cors);
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(config -> config.configurationSource(origins))
                .requestCache(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(config -> config
                        .authenticationEntryPoint((req, res, ex) -> errors.resolveException(req, res, null, ex))
                        .accessDeniedHandler((req, res, ex) -> errors.resolveException(req, res, null, ex)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/me/preferences").hasRole("PATIENT")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(tokens, users), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
