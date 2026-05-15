package ro.ophthacloud.modules.reports.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record IopTrendDto(
        UUID patientId,
        String patientName,
        List<IopTrendData> series
) {
    public record IopTrendData(
            LocalDate date,
            UUID consultationId,
            IopMeasurement od,
            IopMeasurement os
    ) {}

    public record IopMeasurement(
            Double iop,
            String method
    ) {}
}
