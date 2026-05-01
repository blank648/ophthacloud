package ro.ophthacloud.modules.prescriptions.internal;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Generates prescription numbers in the format: {PREFIX}-{YYYY}-{MRN-suffix}
 * e.g. RX-2026-004821
 *
 * If a collision is detected, appends a suffix: RX-2026-004821-2, RX-2026-004821-3, etc.
 * Per GUIDE_06 §5.1.
 */
@Component
public class PrescriptionNumberGenerator {

    private static final String PREFIX = "RX";

    /**
     * Generates a unique prescription number.
     *
     * @param mrn      The patient's MRN e.g. "OC-004821"
     * @param tenantId The current tenant
     * @param repository Used to check for collisions within the tenant
     */
    public String generate(String mrn, UUID tenantId, PrescriptionRepository repository) {
        String mrnSuffix = mrn.replace("OC-", "");
        int year = LocalDate.now().getYear();
        String base = String.format("%s-%d-%s", PREFIX, year, mrnSuffix);

        long existingCount = repository.countByTenantIdAndPrescriptionNumberStartingWith(tenantId, base);

        if (existingCount == 0) {
            return base;
        }
        // Collision: append -2, -3, etc.
        return base + "-" + (existingCount + 1);
    }
}
