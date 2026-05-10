package ro.ophthacloud.modules.optical;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ro.ophthacloud.modules.optical.dto.UpdateStockRequest;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for StockController.
 */
@DisplayName("StockController integration")
class StockControllerIntegrationTest extends BaseIntegrationTest {

    private static final UUID TENANT_A = UUID.randomUUID();

    @BeforeEach
    void setup() {
        ensureTenantExists(TENANT_A);
    }

    @Test
    @DisplayName("GET /api/v1/optical/stock: should list all items")
    void listStock_shouldReturnItems() {
        createStockItem("Item 1", 10, TENANT_A);
        createStockItem("Item 2", 20, TENANT_A);

        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/v1/optical/stock")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> data = (List<?>) response.getBody().get("data");
        assertThat(data).hasSize(2);
    }

    @Test
    @DisplayName("GET /api/v1/optical/stock/low-stock: should return only low stock items")
    void getLowStockReport_shouldFilter() {
        createStockItem("High Stock", 10, TENANT_A); // min is 5
        createStockItem("Low Stock", 3, TENANT_A);

        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/v1/optical/stock/low-stock")
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> data = (List<?>) response.getBody().get("data");
        assertThat(data).hasSize(1);
    }

    @Test
    @DisplayName("PATCH /api/v1/optical/stock/{id}/level: should update current stock")
    void updateStockLevel_shouldSucceed() {
        UUID itemId = createStockItem("Item X", 10, TENANT_A);

        ResponseEntity<Map<String, Object>> response = client.patch()
                .uri("/api/v1/optical/stock/{id}/level", itemId)
                .headers(h -> h.addAll(headersForRole("DOCTOR", TENANT_A)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateStockRequest(15))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("currentStock")).isEqualTo(15);
    }

    private UUID createStockItem(String name, int quantity, UUID tenantId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO stock_items (id, tenant_id, name, category, current_stock, minimum_stock, unit_price, currency, created_at, updated_at) " +
            "VALUES (?, ?, ?, 'FRAMES', ?, 5, 100.00, 'RON', NOW(), NOW())",
            id, tenantId, name, quantity
        );
        return id;
    }
}
