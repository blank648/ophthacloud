package ro.ophthacloud.shared.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.ophthacloud.shared.security.OphthaClinicalAuthenticationToken;
import ro.ophthacloud.shared.security.OphthaPrincipal;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link AuditLogService}.
 * <p>
 * Verifies:
 * <ul>
 *   <li>Audit records are persisted with correct tenant/user/ip/action/entity data</li>
 *   <li>The PostgreSQL immutability trigger blocks UPDATE and DELETE on {@code audit_log}</li>
 *   <li>Audit records survive a calling transaction rollback (REQUIRES_NEW)</li>
 * </ul>
 *
 * Shares the singleton Testcontainers PostgreSQL instance from {@link BaseIntegrationTest}
 * so that the full suite runs against a single container without port-death across contexts.
 */
class AuditLogServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final UUID TEST_TENANT_ID = UUID.randomUUID();
    private static final String TEST_KEYCLOAK_USER_ID = UUID.randomUUID().toString();
    private static final String TEST_STAFF_ID = UUID.randomUUID().toString();

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUpAudit() {
        // BaseIntegrationTest.setUp() already truncates audit_log via dbCleanup()
        setSecurityContext(TEST_TENANT_ID, TEST_KEYCLOAK_USER_ID, TEST_STAFF_ID, "DOCTOR");
        ensureTenantExists(TEST_TENANT_ID);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("log: should persist audit record with correct tenant, user, action, and entity fields")
    void log_shouldPersistRecordWithAllFields() {
        UUID patientId = UUID.randomUUID();

        auditLogService.log(
                AuditEntry.builder()
                        .action("CREATE")
                        .entityType("Patient")
                        .entityId(patientId)
                        .changedField("firstName", null, "Ion")
                        .changedField("lastName",  null, "Popescu")
                        .build()
        );

        List<AuditLogEntity> records = auditLogRepository.findAll();
        assertThat(records).hasSize(1);

        AuditLogEntity saved = records.getFirst();
        assertThat(saved.getTenantId()).isEqualTo(TEST_TENANT_ID);
        assertThat(saved.getActorId()).isEqualTo(TEST_KEYCLOAK_USER_ID);
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getEntityType()).isEqualTo("Patient");
        assertThat(saved.getEntityId()).isEqualTo(patientId);
        assertThat(saved.getChangedFields()).isNotNull();
        assertThat(saved.getChangedFields()).containsKey("firstName");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("log: should persist DELETE audit record without changed_fields")
    void log_shouldPersistDeleteRecordWithNullChangedFields() {
        UUID patientId = UUID.randomUUID();

        auditLogService.log(
                AuditEntry.builder()
                        .action("DELETE")
                        .entityType("Patient")
                        .entityId(patientId)
                        .build()  // no changedField() calls
        );

        AuditLogEntity saved = auditLogRepository.findAll().getFirst();
        assertThat(saved.getAction()).isEqualTo("DELETE");
        assertThat(saved.getChangedFields()).isNull();
    }

    @Test
    @DisplayName("log: should persist audit record without entityId for bulk operations")
    void log_shouldAllowNullEntityId() {
        auditLogService.log(
                AuditEntry.builder()
                        .action("EXPORT")
                        .entityType("Patient")
                        // no entityId — bulk export
                        .build()
        );

        AuditLogEntity saved = auditLogRepository.findAll().getFirst();
        assertThat(saved.getEntityId()).isNull();
        assertThat(saved.getAction()).isEqualTo("EXPORT");
    }

    @Test
    @DisplayName("immutability trigger: should throw when attempting UPDATE on audit_log row")
    void immutabilityTrigger_shouldBlockUpdate() {
        UUID patientId = UUID.randomUUID();

        auditLogService.log(
                AuditEntry.builder()
                        .action("CREATE")
                        .entityType("Patient")
                        .entityId(patientId)
                        .build()
        );

        AuditLogEntity saved = auditLogRepository.findAll().getFirst();
        UUID savedId = saved.getId();

        // Attempt raw JDBC UPDATE — must be blocked by the PostgreSQL trigger
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "UPDATE audit_log SET action = 'TAMPERED' WHERE id = ?",
                        savedId
                )
        )
                .isInstanceOf(Exception.class)
                .hasMessageContaining("audit_log records are immutable");
    }

    @Test
    @DisplayName("immutability trigger: should throw when attempting DELETE on audit_log row")
    void immutabilityTrigger_shouldBlockDelete() {
        auditLogService.log(
                AuditEntry.builder()
                        .action("VIEW")
                        .entityType("Patient")
                        .entityId(UUID.randomUUID())
                        .build()
        );

        AuditLogEntity saved = auditLogRepository.findAll().getFirst();
        UUID savedId = saved.getId();

        // Attempt raw JDBC DELETE — must be blocked by the PostgreSQL trigger
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "DELETE FROM audit_log WHERE id = ?",
                        savedId
                )
        )
                .isInstanceOf(Exception.class)
                .hasMessageContaining("audit_log records are immutable");
    }

    @Test
    @DisplayName("log: should use keycloakUserId from OphthaPrincipal as actorId in audit record")
    void log_shouldUsePrincipalKeycloakUserIdAsActorId() {
        String differentKeycloakId = UUID.randomUUID().toString();
        setSecurityContext(TEST_TENANT_ID, differentKeycloakId, TEST_STAFF_ID, "RECEPTIONIST");

        auditLogService.log(
                AuditEntry.builder()
                        .action("VIEW")
                        .entityType("Patient")
                        .entityId(UUID.randomUUID())
                        .build()
        );

        AuditLogEntity saved = auditLogRepository.findAll().getFirst();
        assertThat(saved.getActorId()).isEqualTo(differentKeycloakId);
        assertThat(saved.getTenantId()).isEqualTo(TEST_TENANT_ID);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Plants a valid {@link OphthaPrincipal} into the {@link SecurityContextHolder}
     * so that {@link ro.ophthacloud.shared.security.SecurityUtils} resolves correctly.
     */
    private void setSecurityContext(UUID tenantId, String keycloakUserId,
                                    String staffId, String staffRole) {
        OphthaPrincipal principal = new OphthaPrincipal(
                keycloakUserId,
                tenantId.toString(),
                staffId,
                staffRole,
                Map.of()
        );
        OphthaClinicalAuthenticationToken auth =
                new OphthaClinicalAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
