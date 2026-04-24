package ro.ophthacloud.modules.emr.dto;

/**
 * Request body for PUT /api/v1/emr/consultations/{id}/sections/{sectionCode} (GUIDE_04 §4.4).
 * sectionData is an arbitrary JSON object (stored as JSONB) validated by SectionDataValidator.
 */
public record SaveSectionRequest(
        String sectionData,
        Boolean isCompleted
) {}
