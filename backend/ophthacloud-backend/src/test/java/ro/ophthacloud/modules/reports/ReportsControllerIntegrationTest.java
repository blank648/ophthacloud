package ro.ophthacloud.modules.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ro.ophthacloud.modules.reports.internal.ReportsController}.
 *
 * Uses Testcontainers (PostgreSQL + Redis) via {@link BaseIntegrationTest}.
 * Covers OC-043 integration tier: dashboard KPIs, appointment statistics, IOP trends.
 */
@DisplayName("ReportsController (Integration)")
class ReportsControllerIntegrationTest extends BaseIntegrationTest {

    private static final UUID TENANT_ID = UUID.randomUUID();

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void seed() {
        ensureTenantExists(TENANT_ID);
    }

    @Override
    protected void dbCleanup() {
        // Clear Redis cache
        try (var conn = redisConnectionFactory.getConnection()) {
            conn.serverCommands().flushAll();
        } catch (Exception ignored) {}

        // Truncate SQL tables
        for (String table : new String[]{
                "invoice_lines", "invoices",
                "optical_order_items", "optical_orders", "stock_items",
                "prescription_lines", "prescriptions",
                "investigation_files", "investigations",
                "consultation_sections", "consultations",
                "appointments", "blocked_slots", "appointment_types",
                "patient_medical_history", "patient_consents", "patient_attachments",
                "patients", "audit_log"}) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE " + table + " CASCADE");
            } catch (Exception ignored) {}
        }
    }

    // ── Test 1: GET /api/v1/reports/dashboard-kpis → 200 ─────────────────────

    @Test
    @DisplayName("GET /api/v1/reports/dashboard-kpis: should return 200 with valid KPI structure")
    void getDashboardKpis_shouldReturn200WithKpiStructure() {
        var response = client.get()
                .uri("/api/v1/reports/dashboard-kpis")
                .headers(h -> h.putAll(headersForRole("ADMIN", TENANT_ID)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).isNotNull()
                .containsKey("todayAppointments")
                .containsKey("weekRevenue")
                .containsKey("activePatients")
                .containsKey("pendingOrders")
                .containsKey("pendingRecalls")
                .containsKey("lowStockItems")
                .containsKey("upcomingAppointments");

        // Validate nested KPI structures exist
        @SuppressWarnings("unchecked")
        Map<String, Object> todayAppts = (Map<String, Object>) data.get("todayAppointments");
        assertThat(todayAppts).containsKey("count").containsKey("completed").containsKey("pending");
        @SuppressWarnings("unchecked")
        Map<String, Object> weekRev = (Map<String, Object>) data.get("weekRevenue");
        assertThat(weekRev).containsKey("amount").containsKey("currency").containsKey("trendPercent");
        @SuppressWarnings("unchecked")
        Map<String, Object> activePatients = (Map<String, Object>) data.get("activePatients");
        assertThat(activePatients).containsKey("count").containsKey("newThisMonth");
        @SuppressWarnings("unchecked")
        Map<String, Object> pendingOrders = (Map<String, Object>) data.get("pendingOrders");
        assertThat(pendingOrders).containsKey("count").containsKey("overdueCount");
    }

    @Test
    @DisplayName("GET /api/v1/reports/dashboard-kpis: with seeded appointments returns correct counts")
    void getDashboardKpis_withSeededAppointments_shouldReturnNonNegativeCounts() {
        // Insert an appointment type and a today's appointment
        UUID apptTypeId = UUID.randomUUID();
        UUID patientId  = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO patients (id, tenant_id, mrn, first_name, last_name, date_of_birth, gender)
                VALUES (?, ?, ?, ?, ?, ?, 'MALE') ON CONFLICT DO NOTHING
                """, patientId, TENANT_ID, "OC-KPI-001", "Ana", "Popescu", LocalDate.of(1975, 5, 10));

        jdbcTemplate.update("""
                INSERT INTO appointment_types (id, tenant_id, name, duration_minutes, color_hex)
                VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING
                """, apptTypeId, TENANT_ID, "Control", 30, "#FF0000");

        jdbcTemplate.update("""
                INSERT INTO appointments (id, tenant_id, patient_id, appointment_type_id, doctor_id, doctor_name, start_at, end_at, status)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 minutes', 'BOOKED')
                """, UUID.randomUUID(), TENANT_ID, patientId, apptTypeId, UUID.randomUUID(), "Dr. Test");

        var response = client.get()
                .uri("/api/v1/reports/dashboard-kpis")
                .headers(h -> h.putAll(headersForRole("ADMIN", TENANT_ID)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        Map<?, ?> todayAppts = (Map<?, ?>) data.get("todayAppointments");

        assertThat(((Number) todayAppts.get("count")).longValue()).isGreaterThanOrEqualTo(1);
    }

    // ── Test 2: GET /api/v1/reports/appointments → 200 ───────────────────────

    @Test
    @DisplayName("GET /api/v1/reports/appointments: with groupBy=week should return 200 with series")
    void getAppointmentStatistics_withGroupByWeek_shouldReturn200() {
        var response = client.get()
                .uri("/api/v1/reports/appointments?from=2026-01-01&to=2026-03-31&groupBy=week")
                .headers(h -> h.putAll(headersForRole("ADMIN", TENANT_ID)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).containsKey("groupBy").containsKey("series").containsKey("byStatus").containsKey("byType");
        assertThat(data.get("groupBy")).isEqualTo("week");
    }

    @Test
    @DisplayName("GET /api/v1/reports/appointments: date range > 1 year should return 400")
    void getAppointmentStatistics_withDateRangeOver1Year_shouldReturn400() {
        var response = client.get()
                .uri("/api/v1/reports/appointments?from=2024-01-01&to=2026-12-31&groupBy=month")
                .headers(h -> h.putAll(headersForRole("ADMIN", TENANT_ID)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Test 3: GET /api/v1/reports/patients/{id}/iop-trends → 200 ───────────

    @Test
    @DisplayName("GET /api/v1/reports/patients/{id}/iop-trends: should return 200 with series array")
    void getIopTrends_shouldReturn200WithSeries() {
        UUID patientId = UUID.randomUUID();

        var response = client.get()
                .uri("/api/v1/reports/patients/" + patientId + "/iop-trends?from=2026-01-01&to=2026-06-01")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).containsKey("patientId").containsKey("series");
        assertThat(data.get("series")).isInstanceOf(java.util.List.class);
    }

    // ── Test 4: GET /api/v1/reports/patients/demographics → 200 ─────────────

    @Test
    @DisplayName("GET /api/v1/reports/patients/demographics: should return 200 with distribution maps")
    void getPatientDemographics_shouldReturn200WithDistributions() {
        var response = client.get()
                .uri("/api/v1/reports/patients/demographics")
                .headers(h -> h.putAll(headersForRole("ADMIN", TENANT_ID)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).containsKey("ageDistribution").containsKey("genderDistribution")
                .containsKey("topDiagnoses").containsKey("registrationTrend");
    }

    // ── Test 5: Unauthenticated access → 401 ─────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/reports/dashboard-kpis: without auth should return 401")
    void getDashboardKpis_withoutAuth_shouldReturn401() {
        var response = client.get()
                .uri("/api/v1/reports/dashboard-kpis")
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }
}
