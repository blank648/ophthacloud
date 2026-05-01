package ro.ophthacloud.modules.prescriptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ro.ophthacloud.modules.prescriptions.internal.PrescriptionController}.
 * <p>
 * Uses Testcontainers (PostgreSQL + Redis) via {@link BaseIntegrationTest}.
 * Covers OC-023 integration tier:
 * 201 create, 200 GET, 200 sign, 409 cancel-double, 404 not-found.
 */
@DisplayName("PrescriptionController (Integration)")
class PrescriptionControllerIntegrationTest extends BaseIntegrationTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private UUID patientId;

    @BeforeEach
    void seed() {
        ensureTenantExists(TENANT_ID);
        // Insert a minimal patient row so FK constraint and MRN lookup work
        patientId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO patients (id, tenant_id, mrn, first_name, last_name, date_of_birth, gender)
                VALUES (?, ?, ?, ?, ?, ?, 'MALE')
                ON CONFLICT DO NOTHING
                """,
                patientId,
                TENANT_ID,
                "OC-004821",
                "Ion",
                "Popescu",
                LocalDate.of(1980, 1, 1));
    }

    @Override
    protected void dbCleanup() {
        for (String table : new String[]{
                "prescription_lines", "prescriptions",
                "appointments", "blocked_slots", "appointment_types",
                "patients", "audit_log"}) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE " + table + " CASCADE");
            } catch (Exception ignored) {}
        }
    }

    // ── Test 1: POST /api/v1/prescriptions → 201 ─────────────────────────────

    @Test
    @DisplayName("POST /api/v1/prescriptions: should create prescription and return 201 with RX number")
    void createPrescription_shouldReturn201_withPrescriptionNumber() {
        var response = client.post()
                .uri("/api/v1/prescriptions")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildCreateRequest())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("prescriptionNumber").toString()).startsWith("RX-");
        assertThat(data.get("status")).isEqualTo("ACTIVE");
        assertThat(data.get("qrCodeToken").toString())
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    // ── Test 2: GET /api/v1/prescriptions/{id} → 200 ─────────────────────────

    @Test
    @DisplayName("GET /api/v1/prescriptions/{id}: should return 200 and the created prescription")
    void getPrescription_shouldReturn200() {
        // Create first
        var createResponse = client.post()
                .uri("/api/v1/prescriptions")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildCreateRequest())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> created = (Map<?, ?>) createResponse.getBody().get("data");
        String id = created.get("id").toString();

        // Fetch it
        var getResponse = client.get()
                .uri("/api/v1/prescriptions/" + id)
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) getResponse.getBody().get("data");
        assertThat(data.get("id").toString()).isEqualTo(id);
        assertThat(data.get("status")).isEqualTo("ACTIVE");
    }

    // ── Test 3: POST /api/v1/prescriptions/{id}/sign → 200 ───────────────────

    @Test
    @DisplayName("POST /api/v1/prescriptions/{id}/sign: should return 200 with signedAt set")
    void signPrescription_shouldReturn200_withSignedAt() {
        // Create first
        var createResponse = client.post()
                .uri("/api/v1/prescriptions")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildCreateRequest())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        String id = ((Map<?, ?>) createResponse.getBody().get("data")).get("id").toString();

        // Sign it
        var signResponse = client.post()
                .uri("/api/v1/prescriptions/" + id + "/sign")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("signatureConfirmation", true))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(signResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) signResponse.getBody().get("data");
        assertThat(data.get("signedAt")).isNotNull();
    }

    // ── Test 4: sign supersedes previous ACTIVE of same type ─────────────────

    @Test
    @DisplayName("signPrescription: signing second DISTANCE prescription supersedes first → first becomes SUPERSEDED")
    void signPrescription_shouldSupersede_previousActivePrescription() {
        // Create and sign first prescription
        var first = client.post()
                .uri("/api/v1/prescriptions")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildCreateRequest())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        String firstId = ((Map<?, ?>) first.getBody().get("data")).get("id").toString();

        client.post()
                .uri("/api/v1/prescriptions/" + firstId + "/sign")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("signatureConfirmation", true))
                .retrieve().toBodilessEntity();

        // Create second prescription of same type
        var second = client.post()
                .uri("/api/v1/prescriptions")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildCreateRequest())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        String secondId = ((Map<?, ?>) second.getBody().get("data")).get("id").toString();

        // Sign second — should supersede first
        client.post()
                .uri("/api/v1/prescriptions/" + secondId + "/sign")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("signatureConfirmation", true))
                .retrieve().toBodilessEntity();

        // Verify first is now SUPERSEDED
        var firstAfter = client.get()
                .uri("/api/v1/prescriptions/" + firstId)
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        Map<?, ?> firstData = (Map<?, ?>) firstAfter.getBody().get("data");
        assertThat(firstData.get("status")).isEqualTo("SUPERSEDED");
    }

    // ── Test 5: POST /api/v1/prescriptions/{id}/cancel → 200 ─────────────────

    @Test
    @DisplayName("POST /api/v1/prescriptions/{id}/cancel: should return 200 and status CANCELLED")
    void cancelPrescription_shouldReturn200() {
        // Create
        var created = client.post()
                .uri("/api/v1/prescriptions")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildCreateRequest())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        String id = ((Map<?, ?>) created.getBody().get("data")).get("id").toString();

        // Cancel
        var cancelResponse = client.post()
                .uri("/api/v1/prescriptions/" + id + "/cancel")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("reason", "Test"))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) cancelResponse.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("CANCELLED");
    }

    // ── Test 6: Double cancel → 409 ──────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/prescriptions/{id}/cancel: second cancel attempt should return 409")
    void cancelPrescription_secondAttempt_shouldReturn409() {
        // Create
        var created = client.post()
                .uri("/api/v1/prescriptions")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildCreateRequest())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        String id = ((Map<?, ?>) created.getBody().get("data")).get("id").toString();

        // Cancel once
        client.post()
                .uri("/api/v1/prescriptions/" + id + "/cancel")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("reason", "First"))
                .retrieve().toBodilessEntity();

        // Cancel again → 409
        var secondCancel = client.post()
                .uri("/api/v1/prescriptions/" + id + "/cancel")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("reason", "Second"))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(secondCancel.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ── Test 7: GET unknown ID → 404 ──────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/prescriptions/{unknownId}: should return 404")
    void getPrescription_withUnknownId_shouldReturn404() {
        var response = client.get()
                .uri("/api/v1/prescriptions/" + UUID.randomUUID())
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Test 8: list prescriptions by patient → 200 ───────────────────────────

    @Test
    @DisplayName("GET /api/v1/prescriptions?patientId=: should return 200 with paginated list")
    void listPrescriptions_shouldReturn200_withPage() {
        // Create one first
        client.post()
                .uri("/api/v1/prescriptions")
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildCreateRequest())
                .retrieve().toBodilessEntity();

        var response = client.get()
                .uri("/api/v1/prescriptions?patientId=" + patientId)
                .headers(h -> h.putAll(headersForRole("DOCTOR", TENANT_ID)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<?, ?>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();
        Map<?, ?> pagination = (Map<?, ?>) body.get("pagination");
        assertThat(pagination).isNotNull();
        assertThat(((Number) pagination.get("totalElements")).intValue()).isGreaterThanOrEqualTo(1);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> buildCreateRequest() {
        Map<String, Object> line = Map.of(
                "eye", "OD",
                "sph", -2.50,
                "cyl", -0.75,
                "axis", 90,
                "vaCc", "6/6"
        );
        return Map.of(
                "patientId", patientId.toString(),
                "prescriptionType", "DISTANCE",
                "validFrom", LocalDate.now().toString(),
                "validUntil", LocalDate.now().plusYears(1).toString(),
                "pdBinocular", 64.0,
                "lensType", "SINGLE_VISION",
                "lines", List.of(line)
        );
    }
}
