package ro.ophthacloud.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Static utility for accessing the current request's authentication data.
 * <p>
 * Per GUIDE_05 §5.3, all service methods must extract {@code tenantId} and {@code staffId}
 * from here — never from request parameters, path variables, or request body.
 * This ensures tenant isolation is enforced even if a caller passes the wrong tenant.
 * <p>
 * Usage:
 * <pre>
 * UUID tenantId = SecurityUtils.currentTenantId();
 * </pre>
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // static utility — prevent instantiation
    }

    /**
     * Returns the current tenant's ID (UUID string form) from the {@link SecurityContextHolder}.
     *
     * @throws IllegalStateException if there is no authenticated {@link OphthaPrincipal} in context
     */
    public static String currentTenantId() {
        return requirePrincipal().tenantId();
    }

    /**
     * Returns the current staff member's ID (UUID string form) from the {@link SecurityContextHolder}.
     *
     * @throws IllegalStateException if there is no authenticated {@link OphthaPrincipal} in context
     */
    public static String currentStaffId() {
        return requirePrincipal().staffId();
    }

    /**
     * Returns the current staff member's role string (e.g. {@code "DOCTOR"}).
     *
     * @throws IllegalStateException if there is no authenticated {@link OphthaPrincipal} in context
     */
    public static String currentStaffRole() {
        return requirePrincipal().staffRole();
    }

    /**
     * Returns the full {@link OphthaPrincipal} for the current request.
     *
     * @throws IllegalStateException if there is no authenticated {@link OphthaPrincipal} in context
     */
    public static OphthaPrincipal currentPrincipal() {
        return requirePrincipal();
    }

    /**
     * Returns {@code true} if the current request has an authenticated {@link OphthaPrincipal}.
     * Safe to call even in unauthenticated contexts (returns false instead of throwing).
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof OphthaPrincipal;
    }

    // ── Private ─────────────────────────────────────────────────────────────────

    private static OphthaPrincipal requirePrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof OphthaPrincipal principal) {
            return principal;
        }
        throw new IllegalStateException(
                "No authenticated OphthaPrincipal in SecurityContextHolder. " +
                "This method must only be called within an authenticated request context.");
    }
}
