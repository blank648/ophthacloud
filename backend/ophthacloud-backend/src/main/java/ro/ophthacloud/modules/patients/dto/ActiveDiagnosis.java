package ro.ophthacloud.modules.patients.dto;

import java.time.LocalDate;

/**
 * Represents an active ICD-10 diagnosis on a patient's medical history.
 * Part of the public API of the patients module — exposed via {@link MedicalHistoryDto}.
 * Moved from {@code internal/} to {@code dto/} so other modules (e.g. appointments) can
 * legally reference it without violating Spring Modulith's dependency rules.
 */
public record ActiveDiagnosis(
    String icd10Code,
    String icd10Name,
    String laterality,
    LocalDate sinceDate
) {}
