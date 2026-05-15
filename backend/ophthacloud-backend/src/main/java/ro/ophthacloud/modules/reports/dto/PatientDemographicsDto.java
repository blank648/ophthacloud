package ro.ophthacloud.modules.reports.dto;

import java.util.List;
import java.util.Map;

public record PatientDemographicsDto(
        Map<String, Long> ageDistribution,
        Map<String, Long> genderDistribution,
        List<DiagnosisData> topDiagnoses,
        List<RegistrationTrendData> registrationTrend
) {
    public record DiagnosisData(
            String icd10Code,
            String description,
            long count
    ) {}

    public record RegistrationTrendData(
            String period,
            long count
    ) {}
}
