package ro.ophthacloud.modules.optical;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ro.ophthacloud.modules.optical.dto.*;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OpticalOrderController.
 */
@DisplayName("OpticalOrderController integration")
class OpticalOrderControllerIntegrationTest extends BaseIntegrationTest {

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    @BeforeEach
    void setup() {
        ensureTenantExists(TENANT_A);
        ensureTenantExists(TENANT_B);
    }

    @Test
    @DisplayName("POST /api/v1/optical/orders: should return 201 and generate order number")
    void createOrder_shouldSucceed() {
        UUID patientId = createPatient(TENANT_A);
        CreateOrderRequest request = new CreateOrderRequest(
                patientId, null, null, OrderType.GLASSES, new BigDecimal("50.00"), null
        );

        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/optical/orders")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("orderNumber")).asString().contains("CMD-");
        assertThat(data.get("stage")).isEqualTo("RECEIVED");
    }

    @Test
    @DisplayName("POST /api/v1/optical/orders/{id}/items: should deduct stock and return 201")
    void addItem_shouldDeductStock() {
        UUID orderId = createOrder(TENANT_A);
        UUID stockItemId = createStockItem("Frame X", 10, TENANT_A);

        AddOrderItemRequest request = new AddOrderItemRequest(
                null, stockItemId, "Frame X description", 1, new BigDecimal("150.00"), BigDecimal.ZERO, "OU", null
        );

        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/optical/orders/{id}/items", orderId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // Verify stock deduction
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT current_stock FROM stock_items WHERE id = ?", Integer.class, stockItemId);
        assertThat(stock).isEqualTo(9);
    }

    @Test
    @DisplayName("PUT /api/v1/optical/orders/{id}/qc: should store QC result")
    void submitQc_shouldSucceed() {
        UUID orderId = createOrder(TENANT_A);
        QcResultDto qc = new QcResultDto(true, true, true, true, true, true, true, true);

        ResponseEntity<Map<String, Object>> response = client.put()
                .uri("/api/v1/optical/orders/{id}/qc", orderId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(qc)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("qcPassedAt")).isNotNull();
    }

    @Test
    @DisplayName("PATCH /api/v1/optical/orders/{id}/stage: should enforce QC gate")
    void updateStage_shouldEnforceQcGate() {
        UUID orderId = createOrder(TENANT_A);
        
        // Move to QC_CHECK first (allowed)
        client.patch()
                .uri("/api/v1/optical/orders/{id}/stage", orderId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .body(new UpdateStageRequest(OrderStage.SENT_TO_LAB, null))
                .retrieve().toBodilessEntity();
        
        client.patch()
                .uri("/api/v1/optical/orders/{id}/stage", orderId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .body(new UpdateStageRequest(OrderStage.QC_CHECK, null))
                .retrieve().toBodilessEntity();

        // Try move to READY_FOR_FITTING without QC -> 422
        ResponseEntity<Map<String, Object>> response = client.patch()
                .uri("/api/v1/optical/orders/{id}/stage", orderId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateStageRequest(OrderStage.READY_FOR_FITTING, null))
                .retrieve()
                .onStatus(s -> s.equals(HttpStatus.UNPROCESSABLE_CONTENT), (req, res) -> {}) // ignore 422
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    @DisplayName("Tenant Isolation: Tenant B cannot view Tenant A's order")
    void tenantIsolation_shouldEnforce() {
        UUID orderId = createOrder(TENANT_A);

        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/v1/optical/orders/{id}", orderId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_B)))
                .retrieve()
                .onStatus(s -> s.equals(HttpStatus.NOT_FOUND), (req, res) -> {})
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UUID createPatient(UUID tenantId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO patients (id, tenant_id, mrn, first_name, last_name, date_of_birth, gender, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, '1990-01-01', 'MALE', NOW(), NOW())",
            id, tenantId, "MRN-" + id.toString().substring(0, 8), "Test", "Patient"
        );
        return id;
    }

    private UUID createOrder(UUID tenantId) {
        UUID patientId = createPatient(tenantId);
        CreateOrderRequest request = new CreateOrderRequest(
                patientId, null, null, OrderType.GLASSES, BigDecimal.ZERO, null
        );
        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/optical/orders")
                .headers(h -> h.addAll(headersForRole("DOCTOR", tenantId)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
        
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        return UUID.fromString((String) data.get("id"));
    }

    private UUID createStockItem(String name, int quantity, UUID tenantId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO stock_items (id, tenant_id, name, category, current_stock, minimum_stock, unit_price, currency, created_at, updated_at) " +
            "VALUES (?, ?, ?, 'FRAMES', ?, ?, ?, ?, NOW(), NOW())",
            id, tenantId, name, quantity, 5, new BigDecimal("100.00"), "RON"
        );
        return id;
    }
}
