package ro.ophthacloud.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Helper for extracting HTTP request metadata from the current servlet request context.
 * <p>
 * Used by {@link ro.ophthacloud.shared.audit.AuditLogService} to capture the client's IP
 * address for every audit record so that operations can be traced back to their source.
 * <p>
 * IP resolution strategy (per GUIDE_06 §11.2):
 * <ol>
 *   <li>Check {@code X-Forwarded-For} header (set by reverse proxies / load balancers)</li>
 *   <li>Check {@code X-Real-IP} header (set by Nginx)</li>
 *   <li>Fall back to {@code HttpServletRequest.getRemoteAddr()}</li>
 * </ol>
 * When {@code X-Forwarded-For} contains a chain of IPs (comma-separated),
 * the <em>first</em> IP is used (the original client; subsequent entries are proxies).
 * <p>
 * Returns {@code null} when invoked outside of a servlet request context
 * (e.g., scheduled tasks, async jobs) — callers must handle this gracefully.
 */
@Component
public class RequestContextHelper {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP       = "X-Real-IP";

    /**
     * Extracts the client IP address from the current HTTP request.
     *
     * @return client IP string, or {@code null} if not in a request context
     */
    public String currentClientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }

        // 1. X-Forwarded-For (may contain comma-separated chain: "clientIp, proxy1, proxy2")
        String xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take only the first IP — the original client
            return xForwardedFor.split(",")[0].trim();
        }

        // 2. X-Real-IP (single value set by Nginx)
        String xRealIp = request.getHeader(HEADER_X_REAL_IP);
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        // 3. Fallback to the direct TCP connection peer address
        return request.getRemoteAddr();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Safely retrieves the current {@link HttpServletRequest}.
     * Returns {@code null} if called outside a servlet request scope.
     */
    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }
}
