package ro.ophthacloud.modules.reports;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.ophthacloud.modules.reports.dto.*;
import ro.ophthacloud.modules.reports.internal.ReportsService;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportsFacade {

    private final ReportsService reportsService;

    public DashboardKpisDto getDashboardKpis(UUID tenantId) {
        return reportsService.getDashboardKpis(tenantId);
    }

    public AppointmentStatisticsDto getAppointmentStatistics(UUID tenantId, LocalDate from, LocalDate to, String groupBy) {
        return reportsService.getAppointmentStatistics(tenantId, from, to, groupBy);
    }

    public RevenueStatisticsDto getRevenueStatistics(UUID tenantId, LocalDate from, LocalDate to, String groupBy) {
        return reportsService.getRevenueStatistics(tenantId, from, to, groupBy);
    }

    public IopTrendDto getIopTrends(UUID tenantId, UUID patientId, LocalDate from, LocalDate to) {
        return reportsService.getIopTrends(tenantId, patientId, from, to);
    }

    public PatientDemographicsDto getPatientDemographics(UUID tenantId) {
        return reportsService.getPatientDemographics(tenantId);
    }
}
