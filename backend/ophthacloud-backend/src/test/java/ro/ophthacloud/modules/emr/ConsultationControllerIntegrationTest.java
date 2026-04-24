package ro.ophthacloud.modules.emr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ro.ophthacloud.modules.emr.internal.ConsultationController}
 * and {@link ro.ophthacloud.modules.emr.internal.ClinicalTemplateController}.
 * <p>
 * Covers OC-020 integration acceptance criteria: full lifecycle
 * POST create → PUT save Section A (verify SEQ) → PUT save Section G
 * → PATCH complete G → POST sign → verify status=SIGNED → PUT save Section A → verify 409.
 */
@DisplayName("ConsultationController integration")
class ConsultationControllerIntegrationTest extends BaseIntegrationTest {

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    private UUID patientId;

    @BeforeEach
    void initConsultationResources() {
        ensureTenantExists(TENANT_A);
        ensureTenantExists(TENANT_B);
        patientId = createPatientAndGetId(TENANT_A);
    }

    @BeforeEach
    void emrDbCleanup() {
        // EMR tables first (FK order), then upstream tables
        for (String table : new String[]{
                "consultation_sections", "clinical_templates", "consultations",
                "appointments", "blocked_slots", "appointment_types", "patients", "audit_log"}) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE " + table + " CASCADE");
            } catch (Exception ignored) { /* table may not exist yet */ }
        }
    }

    // ── Test 1: POST → 201 with 9 sections ───────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/emr/consultations: should return 201 with 9 sections (A–I) in sections map")
    void createConsultation_shouldReturn201_with9Sections() {
        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/emr/consultations")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("id")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> typedSections = (Map<String, Object>) data.get("sections");
        assertThat(typedSections).isNotNull().hasSize(9);
        List.of("A", "B", "C", "D", "E", "F", "G", "H", "I").forEach(code ->
                assertThat(typedSections).containsKey(code));
    }

    // ── Test 2: PUT Section A → SEQ auto-computed ────────────────────────────

    @Test
    @DisplayName("PUT /sections/A: should return section with computed od.seq and os.seq")
    void saveSection_shouldComputeSeq_forSectionA() {
        UUID consultationId = createConsultationAndGetId(TENANT_A);

        String sectionAData = """
                {
                  "od": {"sph": -2.50, "cyl": -0.75, "axis": 90},
                  "os": {"sph": 1.00, "cyl": -0.50, "axis": 180}
                }
                """;

        ResponseEntity<Map<String, Object>> response = client.put()
                .uri("/api/v1/emr/consultations/{id}/sections/{code}", consultationId, "A")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("sectionData", sectionAData, "isCompleted", false))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data).isNotNull();

        String returnedData = (String) data.get("sectionData");
        // SEQ for OD: -2.50 + (-0.75/2) = -2.875 → -2.75
        // SEQ for OS: 1.00 + (-0.50/2) = 0.75 → 0.75
        assertThat(returnedData).contains("\"seq\"");
        assertThat(returnedData).contains("-2.75");
        assertThat(returnedData).contains("0.75");
    }

    // ── Test 3: GET /consultations/{id} → full detail ────────────────────────

    @Test
    @DisplayName("GET /api/v1/emr/consultations/{id}: should return 200 with full consultation and all section data")
    void getConsultation_shouldReturn200_withAllSections() {
        UUID consultationId = createConsultationAndGetId(TENANT_A);

        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/v1/emr/consultations/{id}", consultationId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        Map<?, ?> sections = (Map<?, ?>) data.get("sections");
        assertThat(sections).hasSize(9);
    }

    // ── Test 4: Full lifecycle → sign → verify 409 on subsequent save ─────────

    @Test
    @DisplayName("Full lifecycle: create → save A → save G → complete G → sign → save A → 409")
    void fullLifecycle_shouldEnforceImmutabilityAfterSign() {
        UUID consultationId = createConsultationAndGetId(TENANT_A);

        // PUT Section A — some data
        client.put()
                .uri("/api/v1/emr/consultations/{id}/sections/{code}", consultationId, "A")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("sectionData", "{\"note\":\"Initial\"}", "isCompleted", false))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        // PUT Section G — diagnostic data
        client.put()
                .uri("/api/v1/emr/consultations/{id}/sections/{code}", consultationId, "G")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("sectionData", "{\"diagnosis\":\"H52.1\"}", "isCompleted", false))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        // PATCH complete Section G
        ResponseEntity<Map<String, Object>> completeResp = client.patch()
                .uri("/api/v1/emr/consultations/{id}/sections/{code}/complete", consultationId, "G")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});
        assertThat(completeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify bitmask includes bit 6 (64)
        Map<?, ?> completeData = (Map<?, ?>) completeResp.getBody().get("data");
        assertThat(((Number) completeData.get("sectionsCompleted")).intValue() & 64).isEqualTo(64);

        // POST sign the consultation
        ResponseEntity<Map<String, Object>> signResp = client.post()
                .uri("/api/v1/emr/consultations/{id}/sign", consultationId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("signatureConfirmation", true))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(signResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> signedData = (Map<?, ?>) signResp.getBody().get("data");
        assertThat(signedData.get("status")).isEqualTo("SIGNED");
        assertThat(signedData.get("signedAt")).isNotNull();

        // PUT Section A after signing → must return 409
        ResponseEntity<Map<String, Object>> afterSignResp = client.put()
                .uri("/api/v1/emr/consultations/{id}/sections/{code}", consultationId, "A")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("sectionData", "{\"note\":\"Attempt after sign\"}", "isCompleted", false))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(afterSignResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Map<?, ?> errorBody = (Map<?, ?>) afterSignResp.getBody().get("error");
        assertThat(errorBody.get("code")).isEqualTo("CONSULTATION_ALREADY_SIGNED");
    }

    // ── Test 5: sign blocked when Section G not complete → 400 ───────────────

    @Test
    @DisplayName("POST /sign: should return 422 when Section G is not completed (sign precondition)")
    void signConsultation_shouldReturn422_whenSectionGNotCompleted() {
        UUID consultationId = createConsultationAndGetId(TENANT_A);

        // Attempt to sign without completing G
        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/emr/consultations/{id}/sign", consultationId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("signatureConfirmation", true))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        Map<?, ?> error = (Map<?, ?>) response.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("PRECONDITION_FAILED");
    }

    // ── Test 6: GET → 404 for unknown consultation ────────────────────────────

    @Test
    @DisplayName("GET /api/v1/emr/consultations/{id}: should return 404 CONSULTATION_NOT_FOUND for unknown id")
    void getConsultation_shouldReturn404_forUnknownId() {
        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/v1/emr/consultations/{id}", UUID.randomUUID())
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<?, ?> error = (Map<?, ?>) response.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("CONSULTATION_NOT_FOUND");
    }

    // ── Test 7: RBAC — RECEPTIONIST cannot create ─────────────────────────────

    @Test
    @DisplayName("RBAC: POST /api/v1/emr/consultations with RECEPTIONIST role should return 403")
    void createConsultation_shouldReturn403_forReceptionist() {
        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/emr/consultations")
                .headers(h -> h.addAll(headersForRole("RECEPTIONIST", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── Test 8: Tenant isolation ──────────────────────────────────────────────

    @Test
    @DisplayName("Tenant isolation: consultation created for tenantA should not appear in tenantB queries")
    void tenantIsolation_shouldReturnNotFound_forOtherTenant() {
        UUID consultationId = createConsultationAndGetId(TENANT_A);

        // Query as TENANT_B — should get 404 (tenant filter hides it)
        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/v1/emr/consultations/{id}", consultationId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_B)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Test 9: Template CRUD + apply ────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/emr/templates: should return 201 with created template")
    void createTemplate_shouldReturn201() {
        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/emr/templates")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "name", "Standard Refraction A",
                        "sectionCode", "A",
                        "templateData", "{\"od\":{\"axis\":90}}",
                        "isGlobal", false
                ))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("id")).isNotNull();
        assertThat(data.get("name")).isEqualTo("Standard Refraction A");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> createRequest() {
        return Map.of(
                "patientId", patientId.toString(),
                "consultationDate", LocalDate.now().toString(),
                "chiefComplaint", "Scădere AV"
        );
    }

    private UUID createConsultationAndGetId(UUID tenantId) {
        Map<String, Object> body = client.post()
                .uri("/api/v1/emr/consultations")
                .headers(h -> h.addAll(headersForRole("DOCTOR", tenantId)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        return UUID.fromString((String) data.get("id"));
    }

    private UUID createPatientAndGetId(UUID tenantId) {
        Map<String, Object> body = client.post()
                .uri("/api/v1/patients")
                .headers(h -> h.addAll(headersForRole("DOCTOR", tenantId)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "firstName", "Ion",
                        "lastName", "Popescu",
                        "dateOfBirth", "1985-06-15",
                        "gender", "MALE"
                ))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        return UUID.fromString((String) data.get("id"));
    }
}
