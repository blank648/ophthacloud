package ro.ophthacloud.modules.optical;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ro.ophthacloud.modules.optical.dto.CreateInvoiceRequest;
import ro.ophthacloud.modules.optical.dto.PayInvoiceRequest;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for InvoiceController.
 */
@DisplayName("InvoiceController integration")
class InvoiceControllerIntegrationTest extends BaseIntegrationTest {

    private static final UUID TENANT_A = UUID.randomUUID();

    @BeforeEach
    void setup() {
        ensureTenantExists(TENANT_A);
    }

    @Test
    @DisplayName("POST /api/v1/optical/invoices: should create invoice from order")
    void createInvoice_shouldSucceed() {
        UUID patientId = createPatient(TENANT_A);
        UUID orderId = createOrder(TENANT_A, patientId);

        CreateInvoiceRequest request = new CreateInvoiceRequest(orderId, patientId);

        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/optical/invoices")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("invoiceNumber")).asString().isNotNull();
        assertThat(data.get("status")).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("PATCH /api/v1/optical/invoices/{id}/pay: should mark as PAID")
    void payInvoice_shouldSucceed() {
        UUID patientId = createPatient(TENANT_A);
        UUID orderId = createOrder(TENANT_A, patientId);
        UUID invoiceId = createInvoice(TENANT_A, orderId, patientId);

        PayInvoiceRequest request = new PayInvoiceRequest(ro.ophthacloud.modules.optical.internal.PaymentMethod.CASH, "TX-123");

        ResponseEntity<Map<String, Object>> response = client.patch()
                .uri("/api/v1/optical/invoices/{id}/pay", invoiceId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("PAID");
        assertThat(data.get("paidAt")).isNotNull();
    }

    private UUID createPatient(UUID tenantId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO patients (id, tenant_id, mrn, first_name, last_name, date_of_birth, gender, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, '1990-01-01', 'MALE', NOW(), NOW())",
            id, tenantId, "MRN-" + id.toString().substring(0, 8), "Test", "Patient"
        );
        return id;
    }

    private UUID createOrder(UUID tenantId, UUID patientId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO optical_orders (id, tenant_id, patient_id, order_number, order_type, stage, total_amount, currency, created_at, updated_at, version) " +
            "VALUES (?, ?, ?, ?, 'GLASSES', 'RECEIVED', 100.00, 'RON', NOW(), NOW(), 0)",
            id, tenantId, patientId, "CMD-2024-" + id.toString().substring(0, 6)
        );
        return id;
    }

    private UUID createInvoice(UUID tenantId, UUID orderId, UUID patientId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO invoices (id, tenant_id, optical_order_id, patient_id, invoice_number, status, total, currency, created_at, updated_at, version) " +
            "VALUES (?, ?, ?, ?, ?, 'DRAFT', 100.00, 'RON', NOW(), NOW(), 0)",
            id, tenantId, orderId, patientId, "INV-2024-" + id.toString().substring(0, 6)
        );
        return id;
    }
}
