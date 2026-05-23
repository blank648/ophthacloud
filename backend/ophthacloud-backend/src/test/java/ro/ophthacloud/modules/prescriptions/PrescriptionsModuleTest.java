package ro.ophthacloud.modules.prescriptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import ro.ophthacloud.OphthacloudBackendApplication;
import ro.ophthacloud.modules.prescriptions.event.PrescriptionSignedEvent;
import ro.ophthacloud.modules.prescriptions.enums.PrescriptionType;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Spring Modulith boundary tests for the {@code prescriptions} module.
 * Verifies:
 * <ul>
 *   <li>Module structural integrity via {@code ApplicationModules.verify()}</li>
 *   <li>{@link PrescriptionSignedEvent} is observable across module boundaries</li>
 * </ul>
 */
@ApplicationModuleTest
@ActiveProfiles("test")
@DisplayName("PrescriptionsModule (Spring Modulith)")
class PrescriptionsModuleTest {

    // ── Test 1: Module structure is valid ─────────────────────────────────────

    @Test
    @DisplayName("module_isValid: ApplicationModules.verify() should pass without exception")
    void module_isValid() {
        assertThatCode(() ->
                ApplicationModules.of(OphthacloudBackendApplication.class).verify()
        ).doesNotThrowAnyException();
    }

    // ── Test 2: PrescriptionSignedEvent is observable across module boundary ──

    @Test
    @DisplayName("prescriptionSignedEvent_isObservable: event published to module boundary")
    void prescriptionSignedEvent_isObservable(Scenario scenario) {
        UUID prescriptionId = UUID.randomUUID();
        UUID patientId      = UUID.randomUUID();
        UUID tenantId       = UUID.randomUUID();

        scenario.publish(new PrescriptionSignedEvent(
                        prescriptionId,
                        patientId,
                        tenantId,
                        PrescriptionType.DISTANCE,
                        "RX-2026-004821",
                        Instant.now()))
                .andWaitForEventOfType(PrescriptionSignedEvent.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.prescriptionId()).isEqualTo(prescriptionId);
                    assertThat(event.patientId()).isEqualTo(patientId);
                    assertThat(event.tenantId()).isEqualTo(tenantId);
                    assertThat(event.prescriptionType()).isEqualTo(PrescriptionType.DISTANCE);
                    assertThat(event.prescriptionNumber()).isEqualTo("RX-2026-004821");
                });
    }
}
