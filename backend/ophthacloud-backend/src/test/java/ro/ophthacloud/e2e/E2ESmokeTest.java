package ro.ophthacloud.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import ro.ophthacloud.modules.appointments.dto.AppointmentRequest;
import ro.ophthacloud.modules.emr.dto.CreateConsultationRequest;
import ro.ophthacloud.modules.emr.dto.SaveSectionRequest;
import ro.ophthacloud.modules.optical.dto.CreateOrderRequest;
import ro.ophthacloud.modules.optical.internal.OrderType;
import ro.ophthacloud.modules.patients.dto.CreatePatientRequest;
import ro.ophthacloud.modules.prescriptions.dto.CreatePrescriptionRequest;
import ro.ophthacloud.modules.prescriptions.dto.PrescriptionLineRequest;
import ro.ophthacloud.modules.prescriptions.enums.PrescriptionType;
import ro.ophthacloud.shared.enums.GenderType;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class E2ESmokeTest extends BaseIntegrationTest {

    @Test
    void testEndToEndSmokeFlow() {
        // 1. Auth -> headersForRole("DOCTOR", tenantId)
        UUID tenantId = UUID.fromString("11111111-0000-0000-0000-000000000001");
        ensureTenantExists(tenantId);
        HttpHeaders headers = headersForRole("DOCTOR", tenantId);

        // 2. Patient -> POST /api/v1/patients
        CreatePatientRequest patientReq = CreatePatientRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(GenderType.MALE)
                .build();
                
        JsonNode patientResp = client.post().uri("/api/v1/patients")
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(patientReq)
                .retrieve()
                .body(JsonNode.class);

        assertThat(patientResp).isNotNull();
        assertThat(patientResp.get("data").get("mrn").asText()).startsWith("OC-");
        UUID patientId = UUID.fromString(patientResp.get("data").get("id").asText());

        // 3. Appointment -> POST /api/v1/appointments
        AppointmentRequest appointmentReq = AppointmentRequest.builder()
                .patientId(patientId)
                .doctorId(UUID.randomUUID())
                .doctorName("Dr. Smoke Test")
                .startAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .durationMinutes(30)
                .build();

        JsonNode apptResp = client.post().uri("/api/v1/appointments")
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(appointmentReq)
                .retrieve()
                .body(JsonNode.class);

        assertThat(apptResp).isNotNull();
        UUID appointmentId = UUID.fromString(apptResp.get("data").get("id").asText());

        // 4. Consultation -> POST /api/v1/emr/consultations
        CreateConsultationRequest consultReq = new CreateConsultationRequest(
                patientId,
                appointmentId,
                LocalDate.now(),
                "Blurry vision"
        );

        JsonNode consultResp = client.post().uri("/api/v1/emr/consultations")
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(consultReq)
                .retrieve()
                .body(JsonNode.class);

        assertThat(consultResp).isNotNull();
        assertThat(consultResp.get("data").get("status").asText()).isEqualTo("DRAFT");
        UUID consultationId = UUID.fromString(consultResp.get("data").get("id").asText());

        // 5. Section G -> PUT /api/v1/emr/consultations/{id}/sections/G
        SaveSectionRequest sectionReq = new SaveSectionRequest(
                "{\"notes\": \"all good\"}",
                true
        );

        client.put().uri("/api/v1/emr/consultations/{id}/sections/{code}", consultationId, "G")
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(sectionReq)
                .retrieve()
                .toBodilessEntity();

        // 6. Sign -> POST /api/v1/emr/consultations/{id}/sign
        JsonNode signResp = client.post().uri("/api/v1/emr/consultations/{id}/sign", consultationId)
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("signatureConfirmation", true))
                .retrieve()
                .body(JsonNode.class);

        assertThat(signResp.get("data").get("status").asText()).isEqualTo("SIGNED");

        // 7. Prescription -> POST /api/v1/prescriptions
        CreatePrescriptionRequest rxReq = CreatePrescriptionRequest.builder()
                .patientId(patientId)
                .consultationId(consultationId)
                .prescriptionType(PrescriptionType.DISTANCE)
                .validFrom(LocalDate.now())
                .validUntil(LocalDate.now().plusYears(1))
                .lines(List.of(
                        PrescriptionLineRequest.builder()
                                .eye("OD")
                                .sph(new BigDecimal("-1.00"))
                                .build()
                ))
                .build();

        JsonNode rxResp = client.post().uri("/api/v1/prescriptions")
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(rxReq)
                .retrieve()
                .body(JsonNode.class);

        assertThat(rxResp.get("data").get("status").asText()).isEqualTo("ACTIVE");
        UUID prescriptionId = UUID.fromString(rxResp.get("data").get("id").asText());

        // 8. Optical -> POST /api/v1/optical/orders
        CreateOrderRequest optReq = CreateOrderRequest.builder()
                .patientId(patientId)
                .prescriptionId(prescriptionId)
                .consultationId(consultationId)
                .orderType(OrderType.GLASSES)
                .depositPaid(BigDecimal.ZERO)
                .build();

        JsonNode optResp = client.post().uri("/api/v1/optical/orders")
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(optReq)
                .retrieve()
                .body(JsonNode.class);

        assertThat(optResp.get("data").get("stage").asText()).isEqualTo("RECEIVED");

        // 9. PDF -> GET /api/v1/prescriptions/{id}/pdf
        JsonNode pdfResp = client.get().uri("/api/v1/prescriptions/{id}/pdf", prescriptionId)
                .headers(h -> h.addAll(headers))
                .retrieve()
                .body(JsonNode.class);

        assertThat(pdfResp.get("data").get("downloadUrl").asText()).isNotBlank();

        // 10. KPIs -> GET /api/v1/reports/dashboard-kpis
        JsonNode kpiResp = client.get().uri("/api/v1/reports/dashboard-kpis")
                .headers(h -> h.addAll(headers))
                .retrieve()
                .body(JsonNode.class);

        assertThat(kpiResp.get("data").get("todayAppointments").asInt()).isGreaterThanOrEqualTo(0);
    }
}
