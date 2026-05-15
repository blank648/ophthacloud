package ro.ophthacloud.modules.reports.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DashboardKpisDto(
        TodayAppointments todayAppointments,
        WeekRevenue weekRevenue,
        ActivePatients activePatients,
        PendingOrders pendingOrders,
        PendingRecalls pendingRecalls,
        LowStockItems lowStockItems,
        List<UpcomingAppointment> upcomingAppointments
) {
    public record TodayAppointments(long count, long completed, long pending) {}
    public record WeekRevenue(BigDecimal amount, String currency, BigDecimal trendPercent) {}
    public record ActivePatients(long count, long newThisMonth) {}
    public record PendingOrders(long count, long overdueCount) {}
    public record PendingRecalls(long count) {}
    public record LowStockItems(long count) {}
    public record UpcomingAppointment(Instant startAt, String patientName, String status, String typeName) {}
}
