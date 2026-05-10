package ro.ophthacloud.modules.notifications.internal;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRuleEngineTest {

    @Mock
    private NotificationRuleRepository ruleRepository;
    @Mock
    private NotificationLogRepository logRepository;
    
    private TemplateRenderer templateRenderer;
    private ObjectMapper objectMapper;
    private NotificationRuleEngine ruleEngine;

    @Captor
    private ArgumentCaptor<NotificationLogEntity> logCaptor;

    @BeforeEach
    void setUp() {
        templateRenderer = new TemplateRenderer(); // Real instance since it's simple
        objectMapper = new ObjectMapper(); // Real instance to parse actual JSON
        ruleEngine = new NotificationRuleEngine(ruleRepository, logRepository, templateRenderer, objectMapper);
    }

    @Test
    @DisplayName("matchingRule_shouldCreatePendingNotificationLog")
    void matchingRule_shouldCreatePendingNotificationLog() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        
        NotificationRuleEntity rule = new NotificationRuleEntity();
        rule.setId(UUID.randomUUID());
        setTenantId(rule, tenantId);
        rule.setIsActive(true);
        rule.setConfigData("""
            {
                "trigger_type": "APPOINTMENT_CONFIRMED",
                "channels": ["EMAIL"],
                "template_email_subject": "Hello {{patientName}}",
                "template_email_body": "Body"
            }
        """);

        when(ruleRepository.findByIsActiveTrue()).thenReturn(List.of(rule));

        ruleEngine.evaluate(tenantId, "APPOINTMENT_CONFIRMED", "test@test.com", patientId, Map.of("patientName", "John"));

        verify(logRepository).save(logCaptor.capture());
        NotificationLogEntity saved = logCaptor.getValue();
        
        assertThat(saved.getPatientId()).isEqualTo(patientId);
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.getSubject()).isEqualTo("Hello John");
        assertThat(saved.getRecipientAddress()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("noMatchingRule_shouldCreateNoEntries")
    void noMatchingRule_shouldCreateNoEntries() {
        UUID tenantId = UUID.randomUUID();
        
        NotificationRuleEntity rule = new NotificationRuleEntity();
        rule.setId(UUID.randomUUID());
        setTenantId(rule, tenantId);
        rule.setIsActive(true);
        rule.setConfigData("""
            {
                "trigger_type": "PRESCRIPTION_SIGNED",
                "channels": ["EMAIL"]
            }
        """);

        when(ruleRepository.findByIsActiveTrue()).thenReturn(List.of(rule));

        ruleEngine.evaluate(tenantId, "APPOINTMENT_CONFIRMED", "test@test.com", UUID.randomUUID(), Map.of());

        verify(logRepository, never()).save(any());
    }

    @Test
    @DisplayName("inactiveRule_shouldBeSkipped")
    void inactiveRule_shouldBeSkipped() {
        UUID tenantId = UUID.randomUUID();
        // The repository only returns active rules. So if the repository doesn't return it, it's skipped.
        // We will mock the repository returning empty list.
        when(ruleRepository.findByIsActiveTrue()).thenReturn(List.of());

        ruleEngine.evaluate(tenantId, "APPOINTMENT_CONFIRMED", "test@test.com", UUID.randomUUID(), Map.of());

        verify(logRepository, never()).save(any());
    }

    @Test
    @DisplayName("multipleActiveRules_shouldCreateMultipleEntries")
    void multipleActiveRules_shouldCreateMultipleEntries() {
        UUID tenantId = UUID.randomUUID();
        
        NotificationRuleEntity rule1 = new NotificationRuleEntity();
        rule1.setId(UUID.randomUUID());
        setTenantId(rule1, tenantId);
        rule1.setIsActive(true);
        rule1.setConfigData("""
            {
                "trigger_type": "APPOINTMENT_CONFIRMED",
                "channels": ["EMAIL"]
            }
        """);

        NotificationRuleEntity rule2 = new NotificationRuleEntity();
        rule2.setId(UUID.randomUUID());
        setTenantId(rule2, tenantId);
        rule2.setIsActive(true);
        rule2.setConfigData("""
            {
                "trigger_type": "APPOINTMENT_CONFIRMED",
                "channels": ["SMS"]
            }
        """);

        when(ruleRepository.findByIsActiveTrue()).thenReturn(List.of(rule1, rule2));

        ruleEngine.evaluate(tenantId, "APPOINTMENT_CONFIRMED", "test@test.com", UUID.randomUUID(), Map.of());

        verify(logRepository, times(2)).save(any());
    }

    private void setTenantId(ro.ophthacloud.infrastructure.persistence.TenantAwareEntity entity, UUID tenantId) {
        try {
            Field field = ro.ophthacloud.infrastructure.persistence.TenantAwareEntity.class.getDeclaredField("tenantId");
            field.setAccessible(true);
            field.set(entity, tenantId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
