package ro.ophthacloud.modules.appointments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import ro.ophthacloud.OphthacloudBackendApplication;
import ro.ophthacloud.modules.appointments.event.AppointmentBookedEvent;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Spring Modulith boundary tests for the {@code appointments} module.
 * <p>
 * Verifies:
 * <ul>
 *   <li>Module structural integrity via {@code ApplicationModules.verify()}</li>
 *   <li>{@link AppointmentBookedEvent} is published and observable across module boundaries
 *       via the Spring Modulith {@link Scenario} DSL</li>
 * </ul>
 */
@ApplicationModuleTest
@ActiveProfiles("test")
@DisplayName("AppointmentsModule (Spring Modulith)")
class AppointmentsModuleTest {

    // ── Test 1: Module structure is valid ─────────────────────────────────────

    @Test
    @DisplayName("module_isValid: ApplicationModules.verify() should pass without exception")
    void module_isValid() {
        assertThatCode(() ->
                ApplicationModules.of(OphthacloudBackendApplication.class).verify()
        ).doesNotThrowAnyException();
    }

    // ── Test 2: AppointmentBookedEvent is observable via module boundary ──────

    /**
     * Uses the Spring Modulith {@link Scenario} DSL to verify that firing an
     * {@link AppointmentBookedEvent} can be observed at the module boundary.
     * This confirms the event is correctly published in the application event
     * infrastructure and that module-level event detection works as expected.
     * <p>
     * The full booking flow that causes the event to be published is covered by
     * {@link AppointmentControllerIntegrationTest}.
     */
    @Test
    @DisplayName("appointmentBookedEvent_isObservable: AppointmentBookedEvent published to module boundary")
    void appointmentBookedEvent_isObservable(Scenario scenario) {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId     = UUID.randomUUID();
        UUID tenantId      = UUID.randomUUID();

        scenario.publish(new AppointmentBookedEvent(
                        appointmentId,
                        patientId,
                        tenantId,
                        Instant.now().plusSeconds(3600),
                        30))
                .andWaitForEventOfType(AppointmentBookedEvent.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.appointmentId()).isEqualTo(appointmentId);
                    assertThat(event.patientId()).isEqualTo(patientId);
                    assertThat(event.tenantId()).isEqualTo(tenantId);
                    assertThat(event.durationMinutes()).isEqualTo(30);
                });
    }
}
