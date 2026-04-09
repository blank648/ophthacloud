package ro.ophthacloud.shared.security;

/**
 * Immutable record representing the six module-level RBAC permissions
 * extracted from the JWT {@code permissions} claim per GUIDE_05 §3.1.
 * <p>
 * An instance of this record exists for every module code in the JWT payload.
 * The {@link OphthaClinicalPermissionEvaluator} (OC-007) reads these booleans
 * to evaluate every {@code @PreAuthorize("hasPermission(...)") } expression.
 */
public record ModulePermissions(
        boolean view,
        boolean create,
        boolean edit,
        boolean delete,
        boolean sign,
        boolean export
) {
    /** A deny-all permissions object — used as safe default when a module is absent from the JWT. */
    public static final ModulePermissions NONE = new ModulePermissions(false, false, false, false, false, false);
}
