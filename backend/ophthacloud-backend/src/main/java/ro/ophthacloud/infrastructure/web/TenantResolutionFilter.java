package ro.ophthacloud.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import ro.ophthacloud.shared.security.OphthaPrincipal;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that propagates the authenticated tenant's ID into {@link TenantContext}.
 * <p>
 * Lifecycle per request:
 * <ol>
 *   <li>Reads the authenticated {@link OphthaPrincipal} from {@link SecurityContextHolder}
 *       (populated upstream by {@link ro.ophthacloud.shared.security.OphthaClinicalJwtConverter})</li>
 *   <li>Calls {@link TenantContext#set(UUID)} with the principal's tenant ID</li>
 *   <li>Continues the filter chain</li>
 *   <li>Clears the context in {@code finally} — thread safety for thread-pool reuse</li>
 * </ol>
 * <p>
 * If no {@link OphthaPrincipal} is present (unauthenticated request, e.g. Swagger/health),
 * the filter skips setting the context. The request will be rejected by
 * {@link ro.ophthacloud.infrastructure.config.SecurityConfig} before reaching any business code.
 * <p>
 * Must be registered AFTER {@code BearerTokenAuthenticationFilter} in the Spring Security chain
 * so that the {@link SecurityContextHolder} is already populated before this filter runs.
 */
@Component
@Slf4j
public class TenantResolutionFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain) throws ServletException, IOException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.getPrincipal() instanceof OphthaPrincipal principal) {
                UUID tenantId = UUID.fromString(principal.tenantId());
                TenantContext.set(tenantId);
                log.debug("TenantContext set: tenantId={}, path={}", tenantId, request.getRequestURI());
            }

            filterChain.doFilter(request, response);

        } finally {
            // Always clear — prevents tenant ID from leaking to the next request on the same thread
            TenantContext.clear();
            log.debug("TenantContext cleared for path={}", request.getRequestURI());
        }
    }
}
