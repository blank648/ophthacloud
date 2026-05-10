package ro.ophthacloud.modules.notifications.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationLogControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificationLogRepository logRepository;

    @Test
    @DisplayName("Should list notification logs")
    void shouldListNotificationLogs() {
        UUID tenantId = UUID.randomUUID();
        ensureTenantExists(tenantId);
        var headers = headersForRole("DOCTOR", tenantId);

        runAsTenant(tenantId, () -> {
            NotificationLogEntity logEntry = new NotificationLogEntity();
            logEntry.setChannel(NotificationChannel.EMAIL);
            logEntry.setStatus(NotificationStatus.SENT);
            logEntry.setRecipientAddress("test@test.com");
            logEntry.setSubject("Test Subject");
            logEntry.setSentAt(Instant.now());
            return logRepository.save(logEntry);
        });

        var listRes = client.get()
                .uri("/api/v1/notifications/log")
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(String.class);

        assertThat(listRes.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(listRes.getBody()).contains("test@test.com");
        assertThat(listRes.getBody()).contains("Test Subject");
        assertThat(listRes.getBody()).contains("SENT");
    }
}
