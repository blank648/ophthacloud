package ro.ophthacloud.shared.security;

import java.util.Map;

/**
 * The authenticated principal for every OphthaCloud API request.
 * <p>
 * Populated by {@link OphthaClinicalJwtConverter} from the Keycloak JWT claims
 * per GUIDE_05 §4.3. Stored in {@link org.springframework.security.core.context.SecurityContextHolder}
 * for the duration of the request.
 * <p>
 * Accessible anywhere in the call stack via {@link SecurityUtils}.
 *
 * @param keycloakUserId Keycloak user UUID (the {@code sub} claim)
 * @param tenantId       Tenant UUID as string (the {@code tenant_id} custom claim) — never null for clinic users
 * @param staffId        Internal staff UUID (the {@code staff_id} custom claim) — may be null for system tokens
 * @param staffRole      Staff role string (the {@code staff_role} custom claim, e.g. {@code "DOCTOR"})
 * @param permissions    Per-module permissions map, keyed by module code (e.g. {@code "patients"})
 */
public record OphthaPrincipal(
        String keycloakUserId,
        String tenantId,
        String staffId,
        String staffRole,
        Map<String, ModulePermissions> permissions
) implements java.security.Principal {

    @Override
    public String getName() {
        return keycloakUserId;
    }
    /**
     * Returns the permissions for a specific module, or {@link ModulePermissions#NONE} if the
     * module is not present in the JWT (deny-by-default).
     */
    public ModulePermissions permissionsFor(String moduleCode) {
        return permissions.getOrDefault(moduleCode, ModulePermissions.NONE);
    }
}
