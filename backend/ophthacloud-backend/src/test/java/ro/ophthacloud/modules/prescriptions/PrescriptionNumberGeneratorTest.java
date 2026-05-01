package ro.ophthacloud.modules.prescriptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.ophthacloud.modules.prescriptions.internal.PrescriptionNumberGenerator;
import ro.ophthacloud.modules.prescriptions.internal.PrescriptionRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PrescriptionNumberGenerator}.
 * <p>
 * Covers OC-023 mandatory test cases (GUIDE_06 §5.1):
 * <ul>
 *   <li>MRN "OC-004821" → "RX-2026-004821" (no collision)</li>
 *   <li>Collision: second prescription for same patient → "RX-2026-004821-2"</li>
 *   <li>OC-prefix stripping is correct</li>
 *   <li>Multiple collisions produce correct suffix (-3)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionNumberGenerator")
class PrescriptionNumberGeneratorTest {

    @Mock
    private PrescriptionRepository repository;

    private final PrescriptionNumberGenerator generator = new PrescriptionNumberGenerator();

    private static final UUID TENANT_ID = UUID.randomUUID();

    // ── Test 1 (MANDATORY) — MRN "OC-004821" → "RX-{year}-004821" ──────────────

    @Test
    @DisplayName("generate: MRN 'OC-004821' with no collision should produce 'RX-2026-004821'")
    void generate_shouldProduceBaseNumber_whenNoCollision() {
        // Arrange: no existing prescription with this prefix
        when(repository.countByTenantIdAndPrescriptionNumberStartingWith(
                eq(TENANT_ID), anyString())
        ).thenReturn(0L);

        // Act
        String result = generator.generate("OC-004821", TENANT_ID, repository);

        // Assert: strip "OC-" prefix and format with current year
        int year = java.time.LocalDate.now().getYear();
        assertThat(result).isEqualTo("RX-" + year + "-004821");
    }

    // ── Test 2 (MANDATORY) — Collision scenario → "-2" suffix ────────────────────

    @Test
    @DisplayName("generate: collision for 'OC-004821' should produce 'RX-2026-004821-2'")
    void generate_shouldAppendSuffix2_whenOneCollisionExists() {
        // Arrange: one prescription already exists with this base
        when(repository.countByTenantIdAndPrescriptionNumberStartingWith(
                eq(TENANT_ID), anyString())
        ).thenReturn(1L);

        // Act
        String result = generator.generate("OC-004821", TENANT_ID, repository);

        // Assert: suffix "-2" appended
        int year = java.time.LocalDate.now().getYear();
        assertThat(result).isEqualTo("RX-" + year + "-004821-2");
    }

    // ── Test 3 — Two existing → suffix "-3" ──────────────────────────────────────

    @Test
    @DisplayName("generate: two collisions for same MRN should produce 'RX-{year}-004821-3'")
    void generate_shouldAppendSuffix3_whenTwoCollisionsExist() {
        when(repository.countByTenantIdAndPrescriptionNumberStartingWith(
                eq(TENANT_ID), anyString())
        ).thenReturn(2L);

        String result = generator.generate("OC-004821", TENANT_ID, repository);

        int year = java.time.LocalDate.now().getYear();
        assertThat(result).isEqualTo("RX-" + year + "-004821-3");
    }

    // ── Test 4 — Different MRN formats produce correct suffix strips ─────────────

    @Test
    @DisplayName("generate: MRN 'OC-000001' with no collision should produce 'RX-{year}-000001'")
    void generate_shouldStripOcPrefix_forFirstPatient() {
        when(repository.countByTenantIdAndPrescriptionNumberStartingWith(
                eq(TENANT_ID), anyString())
        ).thenReturn(0L);

        String result = generator.generate("OC-000001", TENANT_ID, repository);

        int year = java.time.LocalDate.now().getYear();
        assertThat(result).isEqualTo("RX-" + year + "-000001");
    }

    // ── Test 5 — Number starts with correct prefix ────────────────────────────────

    @Test
    @DisplayName("generate: result always starts with 'RX-'")
    void generate_resultAlwaysStartsWithRx() {
        when(repository.countByTenantIdAndPrescriptionNumberStartingWith(
                eq(TENANT_ID), anyString())
        ).thenReturn(0L);

        String result = generator.generate("OC-012345", TENANT_ID, repository);

        assertThat(result).startsWith("RX-");
    }
}
