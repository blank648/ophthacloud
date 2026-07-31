package ro.ophthacloud.modules.reports.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Real clinical statistics derived from signed consultations.
 * Replaces the previously hardcoded "VA post-refracție" and "Timp consultație per medic"
 * charts on the Reports page with values computed from actual patient records.
 */
public record ClinicalStatisticsDto(
        List<VaDistributionData> vaDistribution,
        List<DoctorDurationData> consultationDurationByDoctor
) {
    /** Distribution of best-corrected visual acuity (post-refraction) across signed consultations. */
    public record VaDistributionData(
            String range,
            long count
    ) {}

    /** Average time (minutes) from opening to signing a consultation, per doctor. */
    public record DoctorDurationData(
            String doctorName,
            BigDecimal avgMinutes
    ) {}
}
