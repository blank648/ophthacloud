package ro.ophthacloud.modules.reports.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.reports.ReportsFacade;
import ro.ophthacloud.modules.reports.dto.*;
import ro.ophthacloud.shared.api.ApiResponse;
import ro.ophthacloud.shared.security.SecurityUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Dashboard KPIs and statistics")
public class ReportsController {

    private final ReportsFacade facade;

    @GetMapping("/dashboard-kpis")
    @Operation(summary = "Get dashboard KPIs", description = "Returns live metrics for the clinic dashboard (cached for 15m)")
    @PreAuthorize("hasPermission('reports', 'MODULE', 'VIEW') or hasPermission('dashboard', 'MODULE', 'VIEW')")
    public ApiResponse<DashboardKpisDto> getDashboardKpis() {
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        return ApiResponse.of(facade.getDashboardKpis(tenantId));
    }

    @GetMapping("/appointments")
    @Operation(summary = "Get appointment statistics", description = "Aggregated appointment data grouped by period")
    @PreAuthorize("hasPermission('reports', 'MODULE', 'VIEW')")
    public ApiResponse<AppointmentStatisticsDto> getAppointmentStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "month") String groupBy) {
        
        validateDateRange(from, to);
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        return ApiResponse.of(facade.getAppointmentStatistics(tenantId, from, to, groupBy));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get revenue statistics", description = "Aggregated revenue data grouped by period")
    @PreAuthorize("hasPermission('reports', 'MODULE', 'VIEW')")
    public ApiResponse<RevenueStatisticsDto> getRevenueStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "month") String groupBy) {
        
        validateDateRange(from, to);
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        return ApiResponse.of(facade.getRevenueStatistics(tenantId, from, to, groupBy));
    }

    @GetMapping("/patients/{id}/iop-trends")
    @Operation(summary = "Get patient IOP trends", description = "Returns chronological IOP measurements for a patient")
    @PreAuthorize("hasPermission('reports', 'MODULE', 'VIEW')")
    public ApiResponse<IopTrendDto> getIopTrends(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        validateDateRange(from, to);
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        return ApiResponse.of(facade.getIopTrends(tenantId, id, from, to));
    }

    @GetMapping("/patients/demographics")
    @Operation(summary = "Get patient demographics", description = "Age, gender and diagnosis distribution")
    @PreAuthorize("hasPermission('reports', 'MODULE', 'VIEW')")
    public ApiResponse<PatientDemographicsDto> getPatientDemographics() {
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        return ApiResponse.of(facade.getPatientDemographics(tenantId));
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw new IllegalArgumentException("Date range cannot exceed 1 year");
        }
    }
}
