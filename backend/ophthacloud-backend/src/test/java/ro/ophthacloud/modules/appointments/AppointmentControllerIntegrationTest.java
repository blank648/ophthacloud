package ro.ophthacloud.modules.appointments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ro.ophthacloud.modules.appointments.dto.AppointmentRequest;
import ro.ophthacloud.modules.appointments.dto.UpdateStatusRequest;
import ro.ophthacloud.shared.enums.AppointmentStatus;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ro.ophthacloud.modules.appointments.internal.AppointmentController}.
 * <p>
 * Covers OC-016 integration acceptance criteria:
 * POST 201 (valid), POST 400 (double-booking), PATCH status chain (BOOKED→CONFIRMED→CHECKED_IN→IN_PROGRESS→COMPLETED),
 * PATCH 400 (COMPLETED→BOOKED), DELETE 204 (BOOKED), DELETE 409 (CHECKED_IN),
 * GET calendar range, RBAC 403, tenant isolation.
 */
@DisplayName("AppointmentController integration")
class AppointmentControllerIntegrationTest extends BaseIntegrationTest {

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    private UUID patientIdA;
    private UUID doctorId;

    @BeforeEach
    void ensureTenantsAndPatient() {
        ensureTenantExists(TENANT_A);
        ensureTenantExists(TENANT_B);
        patientIdA = createPatientViaDb(TENANT_A);
        doctorId   = UUID.randomUUID();
    }

    // ── Test 1: POST → 201 (valid) ────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/appointments: should return 201 with AppointmentDto on valid request")
    void createAppointment_shouldReturn201_onValidRequest() {
        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/appointments")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("id")).isNotNull();
        assertThat(data.get("durationMinutes")).isEqualTo(30);
        assertThat(data.get("endAt")).isNotNull();
    }

    // ── Test 2: POST → 400 DOUBLE_BOOKING ────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/appointments: should return 400 DOUBLE_BOOKING when slot is occupied")
    void createAppointment_shouldReturn400_onDoubleBooking() {
        // Create the first appointment to occupy the slot
        client.post()
                .uri("/api/v1/appointments")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest())
                .retrieve()
                .toEntity(Map.class);

        // Second request for the same slot should conflict
        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/appointments")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> error = (Map<?, ?>) response.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("DOUBLE_BOOKING");
    }

    // ── Test 3: Full status chain BOOKED→CONFIRMED→CHECKED_IN→IN_PROGRESS→COMPLETED ─

    @Test
    @DisplayName("PATCH /api/v1/appointments/{id}/status: should complete full state-machine chain")
    void updateStatus_shouldCompleteFullStateMachineChain() {
        UUID id = createAppointmentAndGetId();

        // BOOKED → CONFIRMED
        patchStatus(id, AppointmentStatus.CONFIRMED, TENANT_A, "DOCTOR");

        // CONFIRMED → CHECKED_IN
        patchStatus(id, AppointmentStatus.CHECKED_IN, TENANT_A, "DOCTOR");

        // CHECKED_IN → IN_PROGRESS
        patchStatus(id, AppointmentStatus.IN_PROGRESS, TENANT_A, "DOCTOR");

        // IN_PROGRESS → COMPLETED
        ResponseEntity<Map<String, Object>> response = client.patch()
                .uri("/api/v1/appointments/{id}/status", id)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateStatusRequest(AppointmentStatus.COMPLETED, null))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("COMPLETED");
    }

    // ── Test 4: PATCH 400 — invalid transition COMPLETED → BOOKED ─────────────

    @Test
    @DisplayName("PATCH /api/v1/appointments/{id}/status: should return 400 INVALID_STATUS_TRANSITION on COMPLETED→BOOKED")
    void updateStatus_shouldReturn400_onInvalidTransitionFromCompleted() {
        UUID id = createAppointmentAndGetId();
        // Advance to COMPLETED
        patchStatus(id, AppointmentStatus.CONFIRMED, TENANT_A, "DOCTOR");
        patchStatus(id, AppointmentStatus.CHECKED_IN, TENANT_A, "DOCTOR");
        patchStatus(id, AppointmentStatus.IN_PROGRESS, TENANT_A, "DOCTOR");
        patchStatus(id, AppointmentStatus.COMPLETED, TENANT_A, "DOCTOR");

        ResponseEntity<Map<String, Object>> response = client.patch()
                .uri("/api/v1/appointments/{id}/status", id)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateStatusRequest(AppointmentStatus.BOOKED, null))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> error = (Map<?, ?>) response.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("INVALID_STATUS_TRANSITION");
    }

    // ── Test 5: DELETE 204 (BOOKED) ───────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/v1/appointments/{id}: should return 204 when appointment is BOOKED")
    void deleteAppointment_shouldReturn204_whenBooked() {
        UUID id = createAppointmentAndGetId();

        ResponseEntity<Void> response = client.delete()
                .uri("/api/v1/appointments/{id}", id)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Subsequent GET returns 404
        ResponseEntity<Map<String, Object>> getResp = client.get()
                .uri("/api/v1/appointments/{id}", id)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Test 6: DELETE 409 (CHECKED_IN) ──────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/v1/appointments/{id}: should return 409 APPOINTMENT_NOT_MODIFIABLE when CHECKED_IN")
    void deleteAppointment_shouldReturn409_whenCheckedIn() {
        UUID id = createAppointmentAndGetId();
        patchStatus(id, AppointmentStatus.CONFIRMED, TENANT_A, "DOCTOR");
        patchStatus(id, AppointmentStatus.CHECKED_IN, TENANT_A, "DOCTOR");

        ResponseEntity<Map<String, Object>> response = client.delete()
                .uri("/api/v1/appointments/{id}", id)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Map<?, ?> error = (Map<?, ?>) response.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("APPOINTMENT_NOT_MODIFIABLE");
    }

    // ── Test 7: GET calendar range ────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/appointments?from=&to=: should return 200 with appointments list in range")
    void listCalendar_shouldReturn200_withAppointmentsInRange() {
        createAppointmentAndGetId();

        // Query the whole day of tomorrow
        String from = java.time.LocalDate.now().plusDays(1).toString();
        String to   = java.time.LocalDate.now().plusDays(2).toString();

        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/v1/appointments?from={from}&to={to}", from, to)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> data = (List<?>) response.getBody().get("data");
        assertThat(data).isNotNull().hasSize(1);
    }

    // ── Test 8: RBAC — RECEPTIONIST cannot CREATE ─────────────────────────────

    @Test
    @DisplayName("RBAC: POST /api/v1/appointments with RECEPTIONIST role should return 403")
    void createAppointment_shouldReturn403_forReceptionist() {
        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/appointments")
                .headers(h -> h.addAll(headersForRole("RECEPTIONIST", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── Test 9: Tenant isolation ──────────────────────────────────────────────

    @Test
    @DisplayName("Tenant isolation: appointment created for tenantA should not appear in tenantB calendar")
    void calendarIsolation_shouldReturnEmptyResult_forOtherTenant() {
        ensureTenantExists(TENANT_B);
        createAppointmentAndGetId(); // creates under TENANT_A

        String from = java.time.LocalDate.now().plusDays(1).toString();
        String to   = java.time.LocalDate.now().plusDays(2).toString();

        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/v1/appointments?from={from}&to={to}", from, to)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_B)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> data = (List<?>) response.getBody().get("data");
        assertThat(data).isNotNull().isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Builds a valid appointment request for tomorrow at 09:00 UTC, 30 minutes. */
    private AppointmentRequest validRequest() {
        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.HOURS);
        return AppointmentRequest.builder()
                .patientId(patientIdA)
                .doctorId(doctorId)
                .doctorName("Dr. Test")
                .startAt(startAt)
                .durationMinutes(30)
                .build();
    }

    /** Creates an appointment via POST and returns its UUID. */
    private UUID createAppointmentAndGetId() {
        Map<String, Object> body = client.post()
                .uri("/api/v1/appointments")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        return UUID.fromString((String) data.get("id"));
    }

    /** PATCH helper — applies a status transition, asserts 200. */
    private void patchStatus(UUID id, AppointmentStatus status, UUID tenantId, String role) {
        ResponseEntity<Map<String, Object>> resp = client.patch()
                .uri("/api/v1/appointments/{id}/status", id)
                .headers(h -> h.addAll(headersForRole(role, tenantId)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateStatusRequest(status, null))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(resp.getStatusCode())
                .as("Expected 200 for transition to %s", status)
                .isEqualTo(HttpStatus.OK);
    }

    /**
     * Creates a patient via HTTP POST and returns its UUID.
     * Uses the same pattern as PatientControllerIntegrationTest.
     */
    private UUID createPatientViaDb(UUID tenantId) {
        Map<String, Object> body = Map.of(
                "firstName", "Ion",
                "lastName", "Popescu",
                "dateOfBirth", "1985-06-15",
                "gender", "MALE"
        );
        Map<String, Object> response = client.post()
                .uri("/api/v1/patients")
                .headers(h -> h.addAll(headersForRole("DOCTOR", tenantId)))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        Map<?, ?> data = (Map<?, ?>) response.get("data");
        return UUID.fromString((String) data.get("id"));
    }
}
