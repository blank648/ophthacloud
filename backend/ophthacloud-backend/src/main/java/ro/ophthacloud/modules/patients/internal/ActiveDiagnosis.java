package ro.ophthacloud.modules.patients.internal;

import java.time.LocalDate;

public record ActiveDiagnosis(
    String icd10Code,
    String icd10Name,
    String laterality,
    LocalDate sinceDate
) {}
