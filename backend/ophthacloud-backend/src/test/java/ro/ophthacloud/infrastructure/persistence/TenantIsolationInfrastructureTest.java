package ro.ophthacloud.infrastructure.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tenant isolation infrastructure tests.
 * <p>
 * NOTE: The data-level isolation test (insert records for 2 tenants → query returns
 * only matching tenant's records) requires actual @Entity classes (PatientEntity)
 * which are created in OC-009. The full data isolation test is activated in OC-012
 * as {@code PatientControllerIntegrationTest} with Testcontainers.
 * <p>
 * This class verifies the infrastructure invariants that underpin tenant isolation:
 * <ul>
 *   <li>TenantContext throws when no tenant is set (guards TenantAwareEntity @PrePersist)</li>
 *   <li>TenantContext is thread-isolated (prevents cross-tenant contamination in thread pools)</li>
 *   <li>TenantContext clears correctly (simulates filter lifecycle)</li>
 * </ul>
 */
@DisplayName("Tenant isolation — infrastructure invariants")
class TenantIsolationInfrastructureTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("TenantAwareEntity.assignTenant: should fail fast when no TenantContext is set")
    void tenantContext_require_shouldThrow_whenNotSet() {
        assertThatThrownBy(TenantContext::require)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TenantContext is empty");
    }

    @Test
    @DisplayName("TenantContext: set and clear should work correctly across the filter lifecycle")
    void tenantContext_shouldSetAndClear_inFilterLifecycle() {
        UUID tenantA = UUID.fromString("11111111-0000-0000-0000-000000000001");

        // Simulate TenantResolutionFilter.doFilterInternal
        try {
            TenantContext.set(tenantA);
            assertThat(TenantContext.get()).isEqualTo(tenantA);
            assertThat(TenantContext.require()).isEqualTo(tenantA);
        } finally {
            TenantContext.clear();
        }

        // After the filter clear, context is empty
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    @DisplayName("TenantContext: different threads hold independent contexts (pool safety)")
    void tenantContext_shouldBeThreadLocal_forPoolSafety() throws InterruptedException {
        UUID tenantA = UUID.fromString("11111111-0000-0000-0000-000000000001");
        UUID tenantB = UUID.fromString("22222222-0000-0000-0000-000000000002");

        TenantContext.set(tenantA);

        UUID[] capturedFromThreadB = {null};
        Thread threadB = new Thread(() -> {
            TenantContext.set(tenantB);
            capturedFromThreadB[0] = TenantContext.get();
            TenantContext.clear();
        });
        threadB.start();
        threadB.join();

        // Thread B's context does not leak into the main thread
        assertThat(TenantContext.get()).isEqualTo(tenantA);
        assertThat(capturedFromThreadB[0]).isEqualTo(tenantB);
    }

    /*
     * TODO OC-012: Full data isolation test
     *
     * Activate this test in OC-012 (PatientControllerIntegrationTest) once PatientEntity exists:
     *
     * @Test
     * @DisplayName("query: should return ONLY records matching TenantContext.get()")
     * void tenantFilter_shouldReturnOnlyCurrentTenantData() {
     *     UUID tenantA = UUID.randomUUID();
     *     UUID tenantB = UUID.randomUUID();
     *
     *     // Insert a patient for tenant A (with TenantContext set to A)
     *     TenantContext.set(tenantA);
     *     patientRepository.save(PatientEntity.create(...));
     *
     *     // Insert a patient for tenant B (with TenantContext set to B)
     *     TenantContext.set(tenantB);
     *     patientRepository.save(PatientEntity.create(...));
     *
     *     // Query as tenant A — must only see tenant A's patient
     *     TenantContext.set(tenantA);
     *     List<PatientEntity> results = patientRepository.findAll();
     *     assertThat(results).hasSize(1);
     *     assertThat(results.get(0).getTenantId()).isEqualTo(tenantA);
     * }
     */
}
