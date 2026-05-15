package ro.ophthacloud.modules.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.ophthacloud.modules.reports.dto.*;
import ro.ophthacloud.modules.reports.internal.ReportsService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ReportsFacade}.
 *
 * Mocks {@link ReportsService} (the service layer) rather than the internal
 * {@link ro.ophthacloud.modules.reports.internal.KpiQueryRepository} directly,
 * since KpiQueryRepository is in the module-private `internal` package.
 *
 * Covers OC-043 acceptance criteria:
 * - dashboard KPIs match seeded counts
 * - IOP trend returns chronologically ordered series
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportsFacade")
class ReportsFacadeTest {

    @Mock
    private ReportsService reportsService;

    private ReportsFacade facade;

    private static final UUID TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        facade = new ReportsFacade(reportsService);
    }

    // ── Dashboard KPI tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("getDashboardKpis: delegates to service and assembles DTO correctly")
    void getDashboardKpis_shouldDelegateAndReturnAssembledDto() {
        var expected = new DashboardKpisDto(
                new DashboardKpisDto.TodayAppointments(24, 8, 16),
                new DashboardKpisDto.WeekRevenue(new BigDecimal("12450.00"), "RON", new BigDecimal("8.5")),
                new DashboardKpisDto.ActivePatients(847, 23),
                new DashboardKpisDto.PendingOrders(12, 2),
                new DashboardKpisDto.PendingRecalls(34),
                new DashboardKpisDto.LowStockItems(5),
                List.of()
        );
        when(reportsService.getDashboardKpis(TENANT_ID)).thenReturn(expected);

        DashboardKpisDto result = facade.getDashboardKpis(TENANT_ID);

        assertThat(result).isSameAs(expected);
        assertThat(result.todayAppointments().count()).isEqualTo(24);
        assertThat(result.todayAppointments().completed()).isEqualTo(8);
        assertThat(result.todayAppointments().pending()).isEqualTo(16);
        assertThat(result.weekRevenue().amount()).isEqualByComparingTo("12450.00");
        assertThat(result.weekRevenue().currency()).isEqualTo("RON");
        assertThat(result.weekRevenue().trendPercent()).isEqualByComparingTo("8.5");
        assertThat(result.activePatients().count()).isEqualTo(847);
        assertThat(result.activePatients().newThisMonth()).isEqualTo(23);
        assertThat(result.pendingOrders().count()).isEqualTo(12);
        assertThat(result.pendingOrders().overdueCount()).isEqualTo(2);
        assertThat(result.pendingRecalls().count()).isEqualTo(34);
        assertThat(result.lowStockItems().count()).isEqualTo(5);
        verify(reportsService, times(1)).getDashboardKpis(TENANT_ID);
    }

    @Test
    @DisplayName("getDashboardKpis: zero values are handled correctly (no nulls)")
    void getDashboardKpis_withZeroCounts_shouldNotFail() {
        var empty = new DashboardKpisDto(
                new DashboardKpisDto.TodayAppointments(0, 0, 0),
                new DashboardKpisDto.WeekRevenue(BigDecimal.ZERO, "RON", BigDecimal.ZERO),
                new DashboardKpisDto.ActivePatients(0, 0),
                new DashboardKpisDto.PendingOrders(0, 0),
                new DashboardKpisDto.PendingRecalls(0),
                new DashboardKpisDto.LowStockItems(0),
                List.of()
        );
        when(reportsService.getDashboardKpis(TENANT_ID)).thenReturn(empty);

        DashboardKpisDto result = facade.getDashboardKpis(TENANT_ID);
        assertThat(result).isNotNull();
        assertThat(result.todayAppointments().count()).isZero();
        assertThat(result.weekRevenue().amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Appointment statistics tests ──────────────────────────────────────────

    @Test
    @DisplayName("getAppointmentStatistics: delegates correctly with groupBy parameter")
    void getAppointmentStatistics_shouldDelegateWithGroupBy() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to   = LocalDate.of(2026, 3, 31);
        var expected   = new AppointmentStatisticsDto(
                "week",
                List.of(new AppointmentStatisticsDto.AppointmentSeriesData("2026-W01", 112, 98, 8, 6)),
                Map.of("COMPLETED", 872L, "NO_SHOW", 45L),
                Map.of("Control Glaucom", 234L)
        );
        when(reportsService.getAppointmentStatistics(TENANT_ID, from, to, "week")).thenReturn(expected);

        AppointmentStatisticsDto result = facade.getAppointmentStatistics(TENANT_ID, from, to, "week");

        assertThat(result.groupBy()).isEqualTo("week");
        assertThat(result.series()).hasSize(1);
        assertThat(result.series().get(0).period()).isEqualTo("2026-W01");
        assertThat(result.series().get(0).total()).isEqualTo(112);
        assertThat(result.byStatus()).containsEntry("COMPLETED", 872L);
        assertThat(result.byType()).containsEntry("Control Glaucom", 234L);
    }

    // ── IOP trend tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getIopTrends: delegates and returns chronologically ordered series with OD+OS")
    void getIopTrends_shouldReturnChronologicallyOrderedSeries() {
        UUID patientId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to   = LocalDate.of(2026, 4, 2);

        var series = List.of(
                new IopTrendDto.IopTrendData(LocalDate.of(2024, 3, 15), UUID.randomUUID(),
                        new IopTrendDto.IopMeasurement(14.0, "GAT"),
                        new IopTrendDto.IopMeasurement(15.0, "GAT")),
                new IopTrendDto.IopTrendData(LocalDate.of(2025, 6, 10), UUID.randomUUID(),
                        new IopTrendDto.IopMeasurement(16.0, "GAT"),
                        new IopTrendDto.IopMeasurement(18.0, "GAT")),
                new IopTrendDto.IopTrendData(LocalDate.of(2026, 1, 20), UUID.randomUUID(),
                        new IopTrendDto.IopMeasurement(17.0, "NCT"),
                        new IopTrendDto.IopMeasurement(19.0, "NCT"))
        );
        IopTrendDto mockDto = new IopTrendDto(patientId, "Gheorghe Ionescu", series);
        when(reportsService.getIopTrends(TENANT_ID, patientId, from, to)).thenReturn(mockDto);

        IopTrendDto result = facade.getIopTrends(TENANT_ID, patientId, from, to);

        assertThat(result.patientId()).isEqualTo(patientId);
        assertThat(result.patientName()).isEqualTo("Gheorghe Ionescu");
        assertThat(result.series()).hasSize(3);

        // Verify series is chronologically ordered (as received from service)
        for (int i = 0; i < result.series().size() - 1; i++) {
            assertThat(result.series().get(i).date())
                    .isBefore(result.series().get(i + 1).date());
        }

        IopTrendDto.IopTrendData first = result.series().get(0);
        assertThat(first.od().iop()).isEqualTo(14.0);
        assertThat(first.os().iop()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("getIopTrends: empty series when no consultation data exists")
    void getIopTrends_withNoData_shouldReturnEmptySeries() {
        UUID patientId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to   = LocalDate.of(2026, 1, 1);

        when(reportsService.getIopTrends(TENANT_ID, patientId, from, to))
                .thenReturn(new IopTrendDto(patientId, "Test Patient", List.of()));

        assertThat(facade.getIopTrends(TENANT_ID, patientId, from, to).series()).isEmpty();
    }
}
