package ro.ophthacloud.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import ro.ophthacloud.shared.security.OphthaClinicalPermissionEvaluator;

/**
 * Registers the custom {@link OphthaClinicalPermissionEvaluator} as the default
 * {@link org.springframework.security.access.PermissionEvaluator} for all
 * {@code @PreAuthorize("hasPermission(...)")} expressions.
 * <p>
 * Separated from {@link SecurityConfig} to keep the filter chain configuration
 * and the method security expression handler configuration distinct.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class MethodSecurityConfig {

    private final OphthaClinicalPermissionEvaluator permissionEvaluator;

    /**
     * Replaces the default no-op PermissionEvaluator with our RBAC implementation.
     * Without this, {@code hasPermission(...)} always returns {@code false}.
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
