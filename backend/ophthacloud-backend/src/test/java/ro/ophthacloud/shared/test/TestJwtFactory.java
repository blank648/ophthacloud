package ro.ophthacloud.shared.test;

import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Static factory for generating signed HS256 test JWTs that are accepted by the
 * application when running under the {@code test} profile.
 *
 * <p>The secret {@value #TEST_SECRET} must match the value set in
 * {@code application-test.yml} under
 * {@code spring.security.oauth2.resourceserver.jwt.secret}.
 *
 * <p>Token claims mirror exactly what {@code OphthaClinicalJwtConverter} expects:
 * <ul>
 *   <li>{@code sub} → keycloakUserId (used by SecurityUtils.currentPrincipal().keycloakUserId())</li>
 *   <li>{@code tenant_id} → tenantId.toString()</li>
 *   <li>{@code staff_id} → staffId</li>
 *   <li>{@code staff_role} → role (e.g. "DOCTOR", "ADMIN", "RECEPTIONIST")</li>
 *   <li>{@code permissions} → per-module permission map</li>
 * </ul>
 */
public final class TestJwtFactory {

    /** Must match {@code spring.security.oauth2.resourceserver.jwt.secret} in application-test.yml.
     *  Must be ≥ 32 chars (256 bits) for HS256 per RFC 7518 §3.2. */
    static final String TEST_SECRET = "test-secret-key-ophthacloud-dev!";

    /** All module codes the permission map covers. */
    private static final List<String> ALL_MODULES = List.of(
            "patients", "appointments", "emr", "prescriptions",
            "investigations", "optical", "reports", "admin",
            "audit", "notifications", "portal", "dashboard"
    );

    private TestJwtFactory() { /* static utility */ }

    /**
     * Creates a signed HS256 JWT with a 1-hour expiry.
     *
     * @param role      e.g. {@code "DOCTOR"}, {@code "ADMIN"}, {@code "RECEPTIONIST"}
     * @param tenantId  the tenant UUID embedded as {@code tenant_id} claim
     * @param staffId   the internal staff UUID string embedded as {@code staff_id} claim
     * @return a compact JWT string suitable for use as a Bearer token in test requests
     */
    public static String createToken(String role, UUID tenantId, String staffId) {
        Instant now  = Instant.now();
        Instant exp  = now.plusSeconds(3600);

        return Jwts.builder()
                .subject(staffId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("tenant_id", tenantId.toString())
                .claim("staff_id",  staffId)
                .claim("staff_role", role)
                .claim("permissions", buildPermissions(role))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Builds the {@code permissions} claim map.
     *
     * <p>DOCTOR and ADMIN receive VIEW/CREATE/EDIT/DELETE on all modules.
     * RECEPTIONIST receives only VIEW on all modules.
     */
    private static Map<String, Object> buildPermissions(String role) {
        boolean fullAccess = "DOCTOR".equals(role) || "ADMIN".equals(role);

        Map<String, Object> permissions = new HashMap<>();
        for (String module : ALL_MODULES) {
            Map<String, Boolean> modulePerms = new HashMap<>();
            modulePerms.put("view",   true);
            modulePerms.put("create", fullAccess);
            modulePerms.put("edit",   fullAccess);
            modulePerms.put("delete", fullAccess);
            modulePerms.put("sign",   fullAccess && ("emr".equals(module) || "prescriptions".equals(module)));
            modulePerms.put("export", fullAccess && ("reports".equals(module) || "audit".equals(module)));
            permissions.put(module, modulePerms);
        }
        return permissions;
    }

    private static SecretKey signingKey() {
        byte[] keyBytes = TEST_SECRET.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}
