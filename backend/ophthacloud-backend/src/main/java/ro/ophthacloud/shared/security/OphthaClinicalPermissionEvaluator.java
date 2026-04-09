package ro.ophthacloud.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * RBAC permission evaluator for all {@code @PreAuthorize("hasPermission(...)")} expressions.
 * <p>
 * Registered as the default {@link PermissionEvaluator} via
 * {@link ro.ophthacloud.infrastructure.config.MethodSecurityConfig}.
 * <p>
 * Usage in controllers (GUIDE_05 §5.2):
 * <pre>
 * {@code @PreAuthorize("hasPermission('patients', 'MODULE', 'VIEW')")}
 * {@code @PreAuthorize("hasPermission('emr', 'MODULE', 'SIGN')")}
 * </pre>
 *
 * The three-argument form {@code hasPermission(moduleCode, targetType, action)} maps to
 * {@link #hasPermission(Authentication, Serializable, String, Object)} where:
 * <ul>
 *   <li>{@code moduleCode} — the module identifier, e.g. {@code "patients"}</li>
 *   <li>{@code targetType} — always {@code "MODULE"} (unused, for SpEL compatibility)</li>
 *   <li>{@code action} — one of {@code VIEW, CREATE, EDIT, DELETE, SIGN, EXPORT}</li>
 * </ul>
 */
@Component
@Slf4j
public class OphthaClinicalPermissionEvaluator implements PermissionEvaluator {

    /**
     * Domain-object-based form — not used in OphthaCloud.
     * Returns {@code false} always to preserve deny-by-default behaviour.
     */
    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        return false;
    }

    /**
     * Module-based permission check — the form used throughout OphthaCloud.
     *
     * @param auth       the current authentication (must contain {@link OphthaPrincipal})
     * @param moduleCode serializable module code, e.g. {@code "patients"}, {@code "emr"}
     * @param targetType always {@code "MODULE"} — used for SpEL expression clarity only
     * @param permission action string: {@code VIEW | CREATE | EDIT | DELETE | SIGN | EXPORT}
     * @return {@code true} if the principal's permissions for the module allow the action
     */
    @Override
    public boolean hasPermission(Authentication auth,
                                 Serializable moduleCode,
                                 String targetType,
                                 Object permission) {
        if (auth == null || !(auth.getPrincipal() instanceof OphthaPrincipal principal)) {
            log.debug("hasPermission: no OphthaPrincipal in Authentication");
            return false;
        }

        String module = String.valueOf(moduleCode);
        String action = String.valueOf(permission);

        ModulePermissions perms = principal.permissions().get(module);
        if (perms == null) {
            log.debug("hasPermission: module '{}' not found in permissions for subject={}",
                    module, principal.keycloakUserId());
            return false;
        }

        boolean granted = switch (action) {
            case "VIEW"   -> perms.view();
            case "CREATE" -> perms.create();
            case "EDIT"   -> perms.edit();
            case "DELETE" -> perms.delete();
            case "SIGN"   -> perms.sign();
            case "EXPORT" -> perms.export();
            default -> {
                log.warn("hasPermission: unknown action '{}' for module '{}' — denying", action, module);
                yield false;
            }
        };

        log.debug("hasPermission: module='{}' action='{}' granted={} for subject={}",
                module, action, granted, principal.keycloakUserId());
        return granted;
    }
}
