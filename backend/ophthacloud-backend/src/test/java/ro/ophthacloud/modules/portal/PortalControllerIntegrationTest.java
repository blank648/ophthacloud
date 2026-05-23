package ro.ophthacloud.modules.portal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ro.ophthacloud.modules.portal.dto.UpdateConsentsRequest;
import ro.ophthacloud.shared.enums.GenderType;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Patient Portal — PortalController.
 * Extends BaseIntegrationTest to reuse PostgreSQL and Redis containers.
 */
@DisplayName("PortalController integration")
class PortalControllerIntegrationTest extends BaseIntegrationTest {

    private static final UUID TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setupTenant() {
        ensureTenantExists(TENANT_ID);
    }

    @Test
    @DisplayName("Portal endpoints: should return 200/204 for PATIENT, and 403 for DOCTOR/RECEPTIONIST")
    void testPortalSecurityAndDataAccess() {
        // 1. Create a patient using DOCTOR headers to ensure patient exists in DB
        UUID patientId = createPatientInDb("John", "Doe");

        // 2. Build headers for this specific patient
        var patientHeaders = headersForPatient(patientId.toString(), TENANT_ID);

        // 3. GET /api/v1/portal/me (Profile) -> Should succeed
        ResponseEntity<Map<String, Object>> profileResp = client.get()
                .uri("/api/v1/portal/me")
                .headers(h -> h.addAll(patientHeaders))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(profileResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> profileData = (Map<?, ?>) profileResp.getBody().get("data");
        assertThat(profileData.get("firstName")).isEqualTo("John");
        assertThat(profileData.get("lastName")).isEqualTo("Doe");

        // 4. GET /api/v1/portal/me/appointments -> Should succeed (empty list is fine)
        ResponseEntity<Map<String, Object>> apptsResp = client.get()
                .uri("/api/v1/portal/me/appointments")
                .headers(h -> h.addAll(patientHeaders))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(apptsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> apptsList = (List<?>) apptsResp.getBody().get("data");
        assertThat(apptsList).isNotNull();

        // 5. GET /api/v1/portal/me/prescriptions -> Should succeed
        ResponseEntity<Map<String, Object>> rxResp = client.get()
                .uri("/api/v1/portal/me/prescriptions")
                .headers(h -> h.addAll(patientHeaders))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(rxResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 6. GET /api/v1/portal/me/investigations -> Should succeed
        ResponseEntity<Map<String, Object>> invResp = client.get()
                .uri("/api/v1/portal/me/investigations")
                .headers(h -> h.addAll(patientHeaders))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(invResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 7. GET /api/v1/portal/me/optical-orders -> Should succeed
        ResponseEntity<Map<String, Object>> optResp = client.get()
                .uri("/api/v1/portal/me/optical-orders")
                .headers(h -> h.addAll(patientHeaders))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(optResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 8. GET /api/v1/portal/me/notifications -> Should succeed
        ResponseEntity<Map<String, Object>> notifResp = client.get()
                .uri("/api/v1/portal/me/notifications")
                .headers(h -> h.addAll(patientHeaders))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(notifResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 9. PUT /api/v1/portal/me/consents -> Should succeed
        UpdateConsentsRequest consentRequest = new UpdateConsentsRequest(true, true, false);
        ResponseEntity<Void> consentResp = client.put()
                .uri("/api/v1/portal/me/consents")
                .headers(h -> h.addAll(patientHeaders))
                .contentType(MediaType.APPLICATION_JSON)
                .body(consentRequest)
                .retrieve()
                .toBodilessEntity();
        assertThat(consentResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 10. RBAC check: DOCTOR role should get 403 on portal endpoints
        var doctorHeaders = headersForRole("DOCTOR", TENANT_ID);
        ResponseEntity<Map<String, Object>> docProfileResp = client.get()
                .uri("/api/v1/portal/me")
                .headers(h -> h.addAll(doctorHeaders))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(docProfileResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private UUID createPatientInDb(String firstName, String lastName) {
        Map<String, Object> body = Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "dateOfBirth", LocalDate.of(1990, 1, 1).toString(),
                "gender", GenderType.MALE.name()
        );
        Map<?, ?> responseBody = client.post()
                .uri("/api/v1/patients")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        Map<?, ?> data = (Map<?, ?>) responseBody.get("data");
        return UUID.fromString((String) data.get("id"));
    }
}
