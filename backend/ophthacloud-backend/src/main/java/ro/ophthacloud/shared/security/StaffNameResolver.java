package ro.ophthacloud.shared.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves a staff member's human-readable display name from {@code staff_members}.
 * <p>
 * Centralizes the lookup that was previously duplicated (and subtly broken) across the
 * Investigations, Prescriptions and EMR controllers/services, where a missing or failed
 * lookup fell back to {@code "Dr. " + staffRole()} — producing labels like
 * {@code "Dr. OPTICAL_TECHNICIAN"} in the UI.
 * <p>
 * The previous inline queries bound {@code keycloak_user_id} (a {@code VARCHAR} column) as a
 * {@link UUID} object, which Postgres rejects ({@code operator does not exist: character varying = uuid}),
 * silently throwing and falling back to the role. This implementation binds the id branch via
 * {@code CAST(? AS uuid)} on a String parameter (null-safe) and the keycloak branch as a plain String,
 * matching the actual column types.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaffNameResolver {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Returns the display name for the principal as {@code "Dr. First Last"}.
     * Falls back to a neutral {@code "Medic curant"} label if the staff row cannot be found —
     * never the raw role string.
     */
    public String resolveDoctorName(OphthaPrincipal principal, UUID tenantId) {
        String fullName = lookupFullName(principal, tenantId);
        if (fullName != null && !fullName.isBlank()) {
            return fullName.startsWith("Dr.") ? fullName : "Dr. " + fullName;
        }
        log.warn("Could not resolve staff name for staffId={} keycloakUserId={} tenant={} — using neutral fallback",
                principal.staffId(), principal.keycloakUserId(), tenantId);
        return "Medic curant";
    }

    /**
     * Returns the raw {@code "First Last"} for the principal, or {@code null} if not found.
     * Matches by internal staff id OR Keycloak user id, scoped to the tenant.
     */
    private static final String NIL_UUID = "00000000-0000-0000-0000-000000000000";

    public String lookupFullName(OphthaPrincipal principal, UUID tenantId) {
        // Non-null sentinels avoid Postgres parameter type-inference issues on null binds.
        String staffId = principal.staffId() != null ? principal.staffId() : NIL_UUID;
        String keycloakUserId = principal.keycloakUserId() != null ? principal.keycloakUserId() : "";
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT first_name || ' ' || last_name FROM staff_members " +
                            "WHERE tenant_id = ? AND (id = CAST(? AS uuid) OR keycloak_user_id = ?) LIMIT 1",
                    String.class,
                    tenantId,
                    staffId,         // String → CAST(? AS uuid); matches staff_members.id
                    keycloakUserId   // String → matches keycloak_user_id VARCHAR column
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            log.warn("StaffNameResolver lookup failed for tenant={}: {}", tenantId, e.getMessage());
            return null;
        }
    }
}
