package ro.ophthacloud.modules.notifications.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ro.ophthacloud.modules.notifications.dto.CreateNotificationRuleRequest;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRuleControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificationRuleRepository ruleRepository;

    @Test
    @DisplayName("Should create and retrieve a notification rule")
    void shouldCreateAndRetrieveRule() {
        UUID tenantId = UUID.randomUUID();
        ensureTenantExists(tenantId);
        var headers = headersForRole("DOCTOR", tenantId);

        // 1. Create
        var createReq = new CreateNotificationRuleRequest(
                "Appointment SMS",
                Map.of("triggerType", "APPOINTMENT_CONFIRMED", "channels", java.util.List.of("SMS")),
                true
        );

        var createRes = client.post()
                .uri("/api/v1/notifications/rules")
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createReq)
                .retrieve()
                .toEntity(String.class);

        assertThat(createRes.getStatusCode().is2xxSuccessful())
                .withFailMessage("Expected 2xx but got %s with body: %s", createRes.getStatusCode(), createRes.getBody())
                .isTrue();

        // 2. List
        var listRes = client.get()
                .uri("/api/v1/notifications/rules")
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(String.class);

        assertThat(listRes.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(listRes.getBody()).contains("Appointment SMS");
        assertThat(listRes.getBody()).contains("APPOINTMENT_CONFIRMED");
    }

    @Test
    @DisplayName("Should toggle rule status")
    void shouldToggleRuleStatus() {
        UUID tenantId = UUID.randomUUID();
        ensureTenantExists(tenantId);
        var headers = headersForRole("DOCTOR", tenantId);

        NotificationRuleEntity rule = new NotificationRuleEntity();
        setTenantId(rule, tenantId);
        rule.setName("Toggle Test");
        rule.setConfigData("{\"triggerType\":\"TEST\"}");
        rule.setIsActive(true);
        
        NotificationRuleEntity savedRule = runAsTenant(tenantId, () -> ruleRepository.save(rule));

        var res = client.patch()
                .uri("/api/v1/notifications/rules/" + savedRule.getId() + "/toggle")
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(String.class);

        assertThat(res.getStatusCode().is2xxSuccessful())
                .withFailMessage("Expected 2xx but got %s with body: %s", res.getStatusCode(), res.getBody())
                .isTrue();
        assertThat(res.getBody()).contains("\"isActive\":false");

        NotificationRuleEntity updated = runAsTenant(tenantId, () -> ruleRepository.findById(savedRule.getId()).orElseThrow());
        assertThat(updated.getIsActive()).isFalse();
    }

    private void setTenantId(ro.ophthacloud.infrastructure.persistence.TenantAwareEntity entity, UUID tenantId) {
        try {
            java.lang.reflect.Field field = ro.ophthacloud.infrastructure.persistence.TenantAwareEntity.class.getDeclaredField("tenantId");
            field.setAccessible(true);
            field.set(entity, tenantId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
