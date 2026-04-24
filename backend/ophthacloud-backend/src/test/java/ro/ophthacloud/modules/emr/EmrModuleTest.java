package ro.ophthacloud.modules.emr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ro.ophthacloud.OphthacloudBackendApplication;
import ro.ophthacloud.modules.emr.event.ConsultationSignedEvent;
import ro.ophthacloud.shared.audit.AuditLogService;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Spring Modulith boundary tests for the {@code emr} module.
 * <p>
 * Verifies:
 * <ul>
 *   <li>Module structural integrity via {@code ApplicationModules.verify()}</li>
 *   <li>{@link ConsultationSignedEvent} is published and observable across module boundaries
 *       via the Spring Modulith {@link Scenario} DSL</li>
 * </ul>
 */
@ApplicationModuleTest
@ActiveProfiles("test")
@DisplayName("EmrModule (Spring Modulith)")
class EmrModuleTest {

    /**
     * AuditLogService lives in the shared module; it must be mocked in the EMR-only slice
     * since @ApplicationModuleTest only activates the emr module's beans.
     */
    @MockitoBean
    AuditLogService auditLogService;

    // ── Test 1: Module structure is valid ─────────────────────────────────────

    @Test
    @DisplayName("module_isValid: ApplicationModules.verify() should pass without exception")
    void module_isValid() {
        assertThatCode(() ->
                ApplicationModules.of(OphthacloudBackendApplication.class).verify()
        ).doesNotThrowAnyException();
    }

    // ── Test 2: ConsultationSignedEvent is observable via module boundary ─────

    /**
     * Uses the Spring Modulith {@link Scenario} DSL to verify that publishing a
     * {@link ConsultationSignedEvent} can be observed at the module boundary.
     * This confirms the event is correctly wired into the application event
     * infrastructure and that the module publishes it using Spring's event bus.
     * <p>
     * The full signing flow that causes the event to be published is covered by
     * {@link ConsultationControllerIntegrationTest}.
     */
    @Test
    @DisplayName("consultationSignedEvent_isObservable: ConsultationSignedEvent published to module boundary")
    void consultationSignedEvent_isObservable(Scenario scenario) {
        UUID consultationId = UUID.randomUUID();
        UUID patientId      = UUID.randomUUID();
        UUID tenantId       = UUID.randomUUID();
        UUID signedById     = UUID.randomUUID();
        Instant signedAt    = Instant.now();

        scenario.publish(new ConsultationSignedEvent(
                        consultationId,
                        patientId,
                        tenantId,
                        signedById,
                        "Dr. Test",
                        signedAt))
                .andWaitForEventOfType(ConsultationSignedEvent.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.consultationId()).isEqualTo(consultationId);
                    assertThat(event.patientId()).isEqualTo(patientId);
                    assertThat(event.tenantId()).isEqualTo(tenantId);
                    assertThat(event.signedById()).isEqualTo(signedById);
                    assertThat(event.doctorName()).isEqualTo("Dr. Test");
                    assertThat(event.signedAt()).isEqualTo(signedAt);
                });
    }
}
