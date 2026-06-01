package ro.ophthacloud.modules.reports.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ro.ophthacloud.modules.reports.dto.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class KpiQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    // ── Dashboard KPIs ──────────────────────────────────────────────────────

    public DashboardKpisDto.TodayAppointments getTodayAppointments(UUID tenantId) {
        String sql = """
            SELECT
                COUNT(*) as total,
                COUNT(*) FILTER (WHERE status IN ('COMPLETED')) as completed,
                COUNT(*) FILTER (WHERE status NOT IN ('COMPLETED','CANCELLED','NO_SHOW')) as pending
            FROM appointments
            WHERE tenant_id = ?
              AND DATE(start_at AT TIME ZONE 'Europe/Bucharest') = DATE(CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Bucharest')
        """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new DashboardKpisDto.TodayAppointments(
                rs.getLong("total"),
                rs.getLong("completed"),
                rs.getLong("pending")
        ), tenantId);
    }

    public DashboardKpisDto.WeekRevenue getWeekRevenue(UUID tenantId) {
        // Current week revenue
        String currentSql = """
            SELECT COALESCE(SUM(total), 0)
            FROM invoices
            WHERE tenant_id = ?
              AND status = 'PAID'
              AND DATE(paid_at) >= DATE_TRUNC('week', CURRENT_DATE)
              AND DATE(paid_at) < DATE_TRUNC('week', CURRENT_DATE) + INTERVAL '7 days'
        """;
        BigDecimal currentRevenue = jdbcTemplate.queryForObject(currentSql, BigDecimal.class, tenantId);
        if (currentRevenue == null) currentRevenue = BigDecimal.ZERO;

        // Previous week revenue
        String prevSql = """
            SELECT COALESCE(SUM(total), 0)
            FROM invoices
            WHERE tenant_id = ?
              AND status = 'PAID'
              AND DATE(paid_at) >= DATE_TRUNC('week', CURRENT_DATE) - INTERVAL '7 days'
              AND DATE(paid_at) < DATE_TRUNC('week', CURRENT_DATE)
        """;
        BigDecimal prevRevenue = jdbcTemplate.queryForObject(prevSql, BigDecimal.class, tenantId);
        if (prevRevenue == null) prevRevenue = BigDecimal.ZERO;

        BigDecimal trendPercent = BigDecimal.ZERO;
        if (prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
            trendPercent = currentRevenue.subtract(prevRevenue)
                    .divide(prevRevenue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        } else if (currentRevenue.compareTo(BigDecimal.ZERO) > 0) {
            trendPercent = new BigDecimal("100");
        }

        return new DashboardKpisDto.WeekRevenue(currentRevenue, "RON", trendPercent);
    }

    public DashboardKpisDto.ActivePatients getActivePatients(UUID tenantId) {
        String sql = """
            SELECT
                COUNT(*) as total,
                COUNT(*) FILTER (WHERE DATE(created_at) >= DATE_TRUNC('month', CURRENT_DATE)) as new_this_month
            FROM patients
            WHERE tenant_id = ?
              AND is_active = true
        """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new DashboardKpisDto.ActivePatients(
                rs.getLong("total"),
                rs.getLong("new_this_month")
        ), tenantId);
    }

    public DashboardKpisDto.PendingOrders getPendingOrders(UUID tenantId) {
        String sql = """
            SELECT
                COUNT(*) as total,
                COUNT(*) FILTER (WHERE stage = 'SENT_TO_LAB' AND DATE(updated_at) < CURRENT_DATE - INTERVAL '14 days') as overdue
            FROM optical_orders
            WHERE tenant_id = ?
              AND stage NOT IN ('COMPLETED','CANCELLED')
        """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new DashboardKpisDto.PendingOrders(
                rs.getLong("total"),
                rs.getLong("overdue")
        ), tenantId);
    }

    public DashboardKpisDto.PendingRecalls getPendingRecalls(UUID tenantId) {
        String sql = """
            SELECT COUNT(*)
            FROM notification_log
            WHERE tenant_id = ?
              AND status = 'PENDING'
        """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
        return new DashboardKpisDto.PendingRecalls(count != null ? count : 0L);
    }

    public DashboardKpisDto.LowStockItems getLowStockItems(UUID tenantId) {
        String sql = """
            SELECT COUNT(*)
            FROM stock_items
            WHERE tenant_id = ?
              AND current_stock <= minimum_stock
        """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
        return new DashboardKpisDto.LowStockItems(count != null ? count : 0L);
    }

    public List<DashboardKpisDto.UpcomingAppointment> getUpcomingAppointments(UUID tenantId) {
        String sql = """
            SELECT
                a.start_at,
                p.first_name || ' ' || p.last_name as patient_name,
                a.status,
                t.name as type_name
            FROM appointments a
            JOIN patients p ON a.patient_id = p.id
            LEFT JOIN appointment_types t ON a.appointment_type_id = t.id
            WHERE a.tenant_id = ?
              AND a.start_at >= CURRENT_TIMESTAMP
              AND a.status NOT IN ('COMPLETED', 'CANCELLED')
            ORDER BY a.start_at ASC
            LIMIT 5
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DashboardKpisDto.UpcomingAppointment(
                rs.getTimestamp("start_at").toInstant(),
                rs.getString("patient_name"),
                rs.getString("status"),
                rs.getString("type_name")
        ), tenantId);
    }

    // ── Reports ─────────────────────────────────────────────────────────────

    public List<AppointmentStatisticsDto.AppointmentSeriesData> getAppointmentSeries(UUID tenantId, LocalDate from, LocalDate to, String groupBy) {
        String format = switch (groupBy.toLowerCase()) {
            case "month" -> "YYYY-MM";
            case "quarter" -> "YYYY-\"Q\"Q";
            case "week" -> "IYYY-\"W\"IW";
            default -> "YYYY-MM";
        };

        String sql = """
            SELECT
                to_char(start_at, ?) as period,
                COUNT(*) as total,
                COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed,
                COUNT(*) FILTER (WHERE status = 'NO_SHOW') as no_show,
                COUNT(*) FILTER (WHERE status = 'CANCELLED') as cancelled
            FROM appointments
            WHERE tenant_id = ?
              AND DATE(start_at) >= ?
              AND DATE(start_at) <= ?
            GROUP BY period
            ORDER BY period ASC
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AppointmentStatisticsDto.AppointmentSeriesData(
                rs.getString("period"),
                rs.getLong("total"),
                rs.getLong("completed"),
                rs.getLong("no_show"),
                rs.getLong("cancelled")
        ), format, tenantId, from, to);
    }

    public Map<String, Long> getAppointmentStatusDistribution(UUID tenantId, LocalDate from, LocalDate to) {
        String sql = """
            SELECT status, COUNT(*) as cnt
            FROM appointments
            WHERE tenant_id = ?
              AND DATE(start_at) >= ?
              AND DATE(start_at) <= ?
            GROUP BY status
        """;
        Map<String, Long> result = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            result.put(rs.getString("status"), rs.getLong("cnt"));
        }, tenantId, from, to);
        return result;
    }

    public Map<String, Long> getAppointmentTypeDistribution(UUID tenantId, LocalDate from, LocalDate to) {
        String sql = """
            SELECT t.name as type_name, COUNT(*) as cnt
            FROM appointments a
            LEFT JOIN appointment_types t ON a.appointment_type_id = t.id
            WHERE a.tenant_id = ?
              AND DATE(a.start_at) >= ?
              AND DATE(a.start_at) <= ?
            GROUP BY t.name
        """;
        Map<String, Long> result = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            String type = rs.getString("type_name");
            result.put(type != null ? type : "Unknown", rs.getLong("cnt"));
        }, tenantId, from, to);
        return result;
    }

    public List<RevenueStatisticsDto.RevenueSeriesData> getRevenueSeries(UUID tenantId, LocalDate from, LocalDate to, String groupBy) {
        String format = switch (groupBy.toLowerCase()) {
            case "month" -> "YYYY-MM";
            case "quarter" -> "YYYY-\"Q\"Q";
            case "week" -> "IYYY-\"W\"IW";
            default -> "YYYY-MM";
        };

        String sql = """
            SELECT
                to_char(paid_at, ?) as period,
                COALESCE(SUM(total), 0) as total
            FROM invoices
            WHERE tenant_id = ?
              AND status = 'PAID'
              AND DATE(paid_at) >= ?
              AND DATE(paid_at) <= ?
            GROUP BY period
            ORDER BY period ASC
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new RevenueStatisticsDto.RevenueSeriesData(
                rs.getString("period"),
                rs.getBigDecimal("total")
        ), format, tenantId, from, to);
    }

    public IopTrendDto getIopTrends(UUID tenantId, UUID patientId, LocalDate from, LocalDate to) {
        // Fetch patient name first
        String patientSql = "SELECT first_name || ' ' || last_name FROM patients WHERE id = ? AND tenant_id = ?";
        String patientName = null;
        try {
            patientName = jdbcTemplate.queryForObject(patientSql, String.class, patientId, tenantId);
        } catch (Exception e) {
            log.warn("Patient not found for IOP trends: {}", patientId);
        }

        String sql = """
            SELECT
                c.consultation_date,
                c.id as consultation_id,
                (cs.section_data -> 'od' ->> 'iop')::float as iop_od,
                (cs.section_data -> 'os' ->> 'iop')::float as iop_os,
                cs.section_data -> 'od' ->> 'method' as method
            FROM consultation_sections cs
            JOIN consultations c ON cs.consultation_id = c.id
            WHERE c.patient_id  = ?
              AND c.tenant_id   = ?
              AND c.status      = 'SIGNED'
              AND cs.section_code = 'C'
              AND DATE(c.consultation_date) >= ?
              AND DATE(c.consultation_date) <= ?
            ORDER BY c.consultation_date ASC
        """;

        List<IopTrendDto.IopTrendData> series = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Double odVal = rs.getObject("iop_od") != null ? rs.getDouble("iop_od") : null;
            Double osVal = rs.getObject("iop_os") != null ? rs.getDouble("iop_os") : null;
            String method = rs.getString("method");

            IopTrendDto.IopMeasurement od = odVal != null ? new IopTrendDto.IopMeasurement(odVal, method) : null;
            IopTrendDto.IopMeasurement os = osVal != null ? new IopTrendDto.IopMeasurement(osVal, method) : null;

            return new IopTrendDto.IopTrendData(
                    rs.getDate("consultation_date").toLocalDate(),
                    UUID.fromString(rs.getString("consultation_id")),
                    od,
                    os
            );
        }, patientId, tenantId, from, to);

        return new IopTrendDto(patientId, patientName, series);
    }

    public PatientDemographicsDto getPatientDemographics(UUID tenantId) {
        // Age Distribution
        String ageSql = """
            SELECT
                CASE
                    WHEN extract(year from age(date_of_birth)) < 18 THEN '0-17'
                    WHEN extract(year from age(date_of_birth)) BETWEEN 18 AND 35 THEN '18-35'
                    WHEN extract(year from age(date_of_birth)) BETWEEN 36 AND 55 THEN '36-55'
                    WHEN extract(year from age(date_of_birth)) > 55 THEN '56+'
                    ELSE 'Unknown'
                END as age_group,
                COUNT(*) as cnt
            FROM patients
            WHERE tenant_id = ? AND is_active = true
            GROUP BY age_group
        """;
        Map<String, Long> ageDist = new HashMap<>();
        jdbcTemplate.query(ageSql, rs -> {
            ageDist.put(rs.getString("age_group"), rs.getLong("cnt"));
        }, tenantId);

        // Gender Distribution
        String genderSql = """
            SELECT gender, COUNT(*) as cnt
            FROM patients
            WHERE tenant_id = ? AND is_active = true
            GROUP BY gender
        """;
        Map<String, Long> genderDist = new HashMap<>();
        jdbcTemplate.query(genderSql, rs -> {
            String gender = rs.getString("gender");
            genderDist.put(gender != null ? gender : "Unknown", rs.getLong("cnt"));
        }, tenantId);

        // Top Diagnoses (simplified, counting occurrences of diagnosis_code in medical history)
        String diagnosisSql = """
            SELECT 
                diag->>'icd10_code' as code, 
                diag->>'icd10_name' as name, 
                COUNT(*) as cnt
            FROM patient_medical_history pmh,
                 jsonb_array_elements(COALESCE(pmh.active_diagnoses, '[]'::jsonb)) diag
            WHERE pmh.tenant_id = ?
            GROUP BY code, name
            ORDER BY cnt DESC
            LIMIT 10
        """;
        List<PatientDemographicsDto.DiagnosisData> topDiagnoses = jdbcTemplate.query(diagnosisSql, (rs, row) -> new PatientDemographicsDto.DiagnosisData(
                rs.getString("code"),
                rs.getString("name"),
                rs.getLong("cnt")
        ), tenantId);

        // Registration Trend (last 6 months)
        String trendSql = """
            SELECT
                to_char(created_at, 'YYYY-MM') as period,
                COUNT(*) as cnt
            FROM patients
            WHERE tenant_id = ?
              AND DATE(created_at) >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '5 months'
            GROUP BY period
            ORDER BY period ASC
        """;
        List<PatientDemographicsDto.RegistrationTrendData> regTrend = jdbcTemplate.query(trendSql, (rs, row) -> new PatientDemographicsDto.RegistrationTrendData(
                rs.getString("period"),
                rs.getLong("cnt")
        ), tenantId);

        return new PatientDemographicsDto(ageDist, genderDist, topDiagnoses, regTrend);
    }
}
