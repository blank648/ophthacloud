package ro.unibuc.fmi.ophthacloud.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary Stage 0 security bootstrap allowing clean local startup before JWT/RBAC wiring.
 * TODO(E1-AD-01): Replace temporary Stage 0 bootstrap filter chain with stateless JWT + RBAC security filter chain.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityBootstrapConfig {

    @Bean
    public SecurityFilterChain securityBootstrapFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
