package ro.ophthacloud.modules.reports.dto;

import java.util.List;
import java.util.Map;

public record AppointmentStatisticsDto(
        String groupBy,
        List<AppointmentSeriesData> series,
        Map<String, Long> byStatus,
        Map<String, Long> byType
) {
    public record AppointmentSeriesData(
            String period,
            long total,
            long completed,
            long noShow,
            long cancelled
    ) {}
}
