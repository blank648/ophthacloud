package ro.ophthacloud.modules.reports.dto;

import java.math.BigDecimal;
import java.util.List;

public record RevenueStatisticsDto(
        String groupBy,
        List<RevenueSeriesData> series
) {
    public record RevenueSeriesData(
            String period,
            BigDecimal total
    ) {}
}
