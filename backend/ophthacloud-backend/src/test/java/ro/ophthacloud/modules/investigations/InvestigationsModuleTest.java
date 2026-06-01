package ro.ophthacloud.modules.investigations;

import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ro.ophthacloud.OphthacloudBackendApplication;
import ro.ophthacloud.modules.investigations.event.InvestigationResultAvailableEvent;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Spring Modulith boundary tests for the {@code investigations} module.
 * Verifies:
 * <ul>
 *   <li>Module structural integrity via {@code ApplicationModules.verify()}</li>
 *   <li>{@link InvestigationResultAvailableEvent} is observable across module boundaries</li>
 * </ul>
 */
@ApplicationModuleTest
@ActiveProfiles("test")
@DisplayName("InvestigationsModule (Spring Modulith)")
class InvestigationsModuleTest {

    @MockitoBean
    private MinioClient minioClient;

    // ── Test 1: Module structure is valid ─────────────────────────────────────

    @Test
    @DisplayName("module_isValid: ApplicationModules.verify() should pass without exception")
    void module_isValid() {
        assertThatCode(() ->
                ApplicationModules.of(OphthacloudBackendApplication.class).verify()
        ).doesNotThrowAnyException();
    }

    // ── Test 2: InvestigationResultAvailableEvent is observable ───────────────

    @Test
    @DisplayName("investigationResultAvailableEvent_isObservable: event published to module boundary")
    void investigationResultAvailableEvent_isObservable(Scenario scenario) {
        UUID investigationId = UUID.randomUUID();
        UUID patientId       = UUID.randomUUID();
        UUID tenantId        = UUID.randomUUID();

        scenario.publish(new InvestigationResultAvailableEvent(
                        investigationId,
                        patientId,
                        tenantId,
                        InvestigationCategoryType.OCT,
                        Instant.now()))
                .andWaitForEventOfType(InvestigationResultAvailableEvent.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.investigationId()).isEqualTo(investigationId);
                    assertThat(event.patientId()).isEqualTo(patientId);
                    assertThat(event.tenantId()).isEqualTo(tenantId);
                    assertThat(event.category()).isEqualTo(InvestigationCategoryType.OCT);
                });
    }
}
