package ro.ophthacloud.modules.reports.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ro.ophthacloud.modules.reports.dto.*;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportsService {

    private final KpiQueryRepository queryRepository;

    @Cacheable(value = "dashboard-kpis", key = "#tenantId")
    public DashboardKpisDto getDashboardKpis(UUID tenantId) {
        log.debug("Computing dashboard KPIs for tenant: {}", tenantId);
        return new DashboardKpisDto(
                queryRepository.getTodayAppointments(tenantId),
                queryRepository.getWeekRevenue(tenantId),
                queryRepository.getActivePatients(tenantId),
                queryRepository.getPendingOrders(tenantId),
                queryRepository.getPendingRecalls(tenantId),
                queryRepository.getLowStockItems(tenantId),
                queryRepository.getUpcomingAppointments(tenantId)
        );
    }

    public AppointmentStatisticsDto getAppointmentStatistics(UUID tenantId, LocalDate from, LocalDate to, String groupBy) {
        return new AppointmentStatisticsDto(
                groupBy,
                queryRepository.getAppointmentSeries(tenantId, from, to, groupBy),
                queryRepository.getAppointmentStatusDistribution(tenantId, from, to),
                queryRepository.getAppointmentTypeDistribution(tenantId, from, to)
        );
    }

    public RevenueStatisticsDto getRevenueStatistics(UUID tenantId, LocalDate from, LocalDate to, String groupBy) {
        return new RevenueStatisticsDto(
                groupBy,
                queryRepository.getRevenueSeries(tenantId, from, to, groupBy)
        );
    }

    public IopTrendDto getIopTrends(UUID tenantId, UUID patientId, LocalDate from, LocalDate to) {
        return queryRepository.getIopTrends(tenantId, patientId, from, to);
    }

    public PatientDemographicsDto getPatientDemographics(UUID tenantId) {
        return queryRepository.getPatientDemographics(tenantId);
    }
}
