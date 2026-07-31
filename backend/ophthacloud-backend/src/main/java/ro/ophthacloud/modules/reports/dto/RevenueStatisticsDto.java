package ro.ophthacloud.modules.reports.dto;

import java.math.BigDecimal;
import java.util.List;

public record RevenueStatisticsDto(
        String groupBy,
        List<RevenueSeriesData> series,
        List<DoctorRevenueData> byDoctor
) {
    public record RevenueSeriesData(
            String period,
            BigDecimal total
    ) {}

    public record DoctorRevenueData(
            String doctorName,
            BigDecimal total
    ) {}
}
