package ro.ophthacloud.modules.patients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ro.ophthacloud.modules.patients.dto.CreatePatientRequest;
import ro.ophthacloud.modules.patients.dto.UpdatePatientRequest;
import ro.ophthacloud.shared.enums.GenderType;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ro.ophthacloud.modules.patients.internal.PatientController}.
 * <p>
 * Extends {@link BaseIntegrationTest} — PostgreSQL 16 + Redis 7 via Testcontainers,
 * HMAC-signed JWT authentication via {@link ro.ophthacloud.shared.test.TestJwtFactory}.
 * HTTP calls use Spring's {@link org.springframework.web.client.RestClient}.
 */
@DisplayName("PatientController integration")
class PatientControllerIntegrationTest extends BaseIntegrationTest {

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    @BeforeEach
    void ensureTenantsExist() {
        ensureTenantExists(TENANT_A);
        ensureTenantExists(TENANT_B);
    }

    // ── Test 1: POST /api/v1/patients → 201, MRN format ──────────────────────

    @Test
    @DisplayName("POST /api/v1/patients: should return 201 with MRN matching OC-\\d{6}")
    void createPatient_shouldReturn201_withMrn() {
        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/patients")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(validCreateRequest("Ion", "Popescu"))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat((String) data.get("mrn")).matches("^OC-\\d{6}$");
    }

    // ── Test 2: POST with invalid body → 400 VALIDATION_ERROR ────────────────

    @Test
    @DisplayName("POST /api/v1/patients: should return 400 VALIDATION_ERROR on invalid body")
    void createPatient_shouldReturn400_onInvalidBody() {
        // firstName null — @NotBlank violation
        Map<String, Object> body = Map.of(
                "lastName", "Popescu",
                "dateOfBirth", "1990-01-01",
                "gender", "MALE"
        );

        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/patients")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> error = (Map<?, ?>) response.getBody().get("error");
        assertThat(error).isNotNull();
        assertThat(error.get("code")).isEqualTo("VALIDATION_ERROR");
    }

    // ── Test 3: GET /api/v1/patients/{id} → 200 ──────────────────────────────

    @Test
    @DisplayName("GET /api/v1/patients/{id}: should return 200 with PatientDto for existing patient")
    void getPatient_shouldReturn200_forExistingPatient() {
        UUID patientId = createPatientAndGetId("Maria", "Ionescu", TENANT_A);

        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/v1/patients/{id}", patientId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("firstName")).isEqualTo("Maria");
        assertThat(data.get("lastName")).isEqualTo("Ionescu");
    }

    // ── Test 4: GET /api/v1/patients/{id} → 404 ──────────────────────────────

    @Test
    @DisplayName("GET /api/v1/patients/{id}: should return 404 PATIENT_NOT_FOUND for unknown id")
    void getPatient_shouldReturn404_forUnknownId() {
        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/v1/patients/{id}", UUID.randomUUID())
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<?, ?> error = (Map<?, ?>) response.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("PATIENT_NOT_FOUND");
    }

    // ── Test 5: PUT /api/v1/patients/{id} → 200 with updated fields ──────────

    @Test
    @DisplayName("PUT /api/v1/patients/{id}: should return 200 with updated fields")
    void updatePatient_shouldReturn200_withUpdatedFields() {
        UUID patientId = createPatientAndGetId("Andrei", "Constantin", TENANT_A);

        UpdatePatientRequest updateReq = new UpdatePatientRequest();
        updateReq.setFirstName("Andrei");
        updateReq.setLastName("Constantinescu-Nou");
        updateReq.setDateOfBirth(LocalDate.of(1985, 6, 15));
        updateReq.setGender(GenderType.MALE);

        ResponseEntity<Map<String, Object>> response = client.put()
                .uri("/api/v1/patients/{id}", patientId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateReq)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("lastName")).isEqualTo("Constantinescu-Nou");
    }

    // ── Test 6: DELETE /api/v1/patients/{id} → 204 ───────────────────────────

    @Test
    @DisplayName("DELETE /api/v1/patients/{id}: should return 204 No Content")
    void deletePatient_shouldReturn204() {
        UUID patientId = createPatientAndGetId("Gheorghe", "Marin", TENANT_A);

        ResponseEntity<Void> deleteResp = client.delete()
                .uri("/api/v1/patients/{id}", patientId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toBodilessEntity();

        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Confirm soft-deleted — subsequent GET returns 404
        ResponseEntity<Map<String, Object>> getResp = client.get()
                .uri("/api/v1/patients/{id}", patientId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Test 7: GET /api/v1/patients?q=Popescu → 200 paginated ──────────────

    @Test
    @DisplayName("GET /api/v1/patients?q=Popescu: should return 200 with pagination")
    void listPatients_shouldReturn200_withPagination() {
        createPatientAndGetId("Ion", "Popescu", TENANT_A);
        createPatientAndGetId("Elena", "Ionescu", TENANT_A);

        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/v1/patients?q=Popescu")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> data = (List<?>) response.getBody().get("data");
        assertThat(data).isNotNull().hasSize(1);
    }

    // ── Test 8: RBAC — RECEPTIONIST cannot POST ───────────────────────────────

    @Test
    @DisplayName("RBAC: POST /api/v1/patients with ROLE_RECEPTIONIST should return 403 Forbidden")
    void createPatient_shouldReturn403_forReceptionist() {
        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/patients")
                .headers(h -> h.addAll(headersForRole("RECEPTIONIST", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(validCreateRequest("Ioana", "Dumitrescu"))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        // RECEPTIONIST has view=true only; create=false → @PreAuthorize fails → 403
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── Test 9: Tenant isolation ──────────────────────────────────────────────

    @Test
    @DisplayName("Tenant isolation: patient created as tenantA should not appear in tenantB queries")
    void tenantIsolation_shouldReturnEmptyResult_forOtherTenant() {
        createPatientAndGetId("Vasile", "Radu", TENANT_A);

        // TenantB queries the patient list — Hibernate tenantFilter must hide TENANT_A rows
        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/v1/patients")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_B)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> data = (List<?>) response.getBody().get("data");
        assertThat(data).isNotNull().isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreatePatientRequest validCreateRequest(String firstName, String lastName) {
        CreatePatientRequest req = new CreatePatientRequest();
        req.setFirstName(firstName);
        req.setLastName(lastName);
        req.setDateOfBirth(LocalDate.of(1985, 6, 15));
        req.setGender(GenderType.MALE);
        return req;
    }

    /**
     * Creates a patient via POST and returns its UUID from the response body.
     */
    private UUID createPatientAndGetId(String firstName, String lastName, UUID tenantId) {
        Map<String, Object> body = client.post()
                .uri("/api/v1/patients")
                .headers(h -> h.addAll(headersForRole("DOCTOR", tenantId)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(validCreateRequest(firstName, lastName))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        return UUID.fromString((String) data.get("id"));
    }
}
