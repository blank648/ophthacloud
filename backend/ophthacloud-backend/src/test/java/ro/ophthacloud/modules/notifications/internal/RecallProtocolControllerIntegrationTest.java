package ro.ophthacloud.modules.notifications.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ro.ophthacloud.modules.notifications.dto.CreateRecallProtocolRequest;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecallProtocolControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should create and retrieve a recall protocol")
    void shouldCreateAndRetrieveProtocol() {
        UUID tenantId = UUID.randomUUID();
        ensureTenantExists(tenantId);
        var headers = headersForRole("DOCTOR", tenantId);

        // 1. Create
        var createReq = new CreateRecallProtocolRequest(
                "Glaucoma 6 Months",
                "H40.1",
                6,
                "Recall for primary open-angle glaucoma",
                true
        );

        var createRes = client.post()
                .uri("/api/v1/notifications/recall-protocols")
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createReq)
                .retrieve()
                .toEntity(String.class);

        assertThat(createRes.getStatusCode().is2xxSuccessful()).isTrue();

        // 2. List
        var listRes = client.get()
                .uri("/api/v1/notifications/recall-protocols")
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(String.class);

        assertThat(listRes.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(listRes.getBody()).contains("Glaucoma 6 Months");
        assertThat(listRes.getBody()).contains("H40.1");
    }
}
