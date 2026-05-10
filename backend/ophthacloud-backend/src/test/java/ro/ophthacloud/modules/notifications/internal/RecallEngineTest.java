package ro.ophthacloud.modules.notifications.internal;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecallEngineTest {

    @Mock
    private RecallProtocolRepository protocolRepository;
    @Mock
    private NotificationRuleRepository ruleRepository;
    @Mock
    private RecallPatientQueryRepository recallQueryRepository;
    @Mock
    private NotificationRuleEngine ruleEngine;

    private ObjectMapper objectMapper;
    private RecallEngine recallEngine;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper(); // Real instance to parse configData
        recallEngine = new RecallEngine(protocolRepository, ruleRepository, recallQueryRepository, ruleEngine, objectMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("recallNeeded_allConditionsMet_shouldCreatePendingNotification")
    void recallNeeded_allConditionsMet_shouldCreatePendingNotification() {
        UUID tenantId = UUID.randomUUID();
        
        RecallProtocolEntity protocol = new RecallProtocolEntity();
        protocol.setId(UUID.randomUUID());
        setTenantId(protocol, tenantId);
        protocol.setIcd10Code("H40.1");
        protocol.setRecallIntervalMonths(6);
        protocol.setIsActive(true);

        NotificationRuleEntity rule = new NotificationRuleEntity();
        rule.setId(UUID.randomUUID());
        setTenantId(rule, tenantId);
        rule.setIsActive(true);
        rule.setConfigData("""
            {
                "triggerType": "RECALL",
                "recallProtocolId": "%s"
            }
        """.formatted(protocol.getId()));

        when(protocolRepository.findAll()).thenReturn(List.of(protocol));
        when(ruleRepository.findByTenantIdAndIsActiveTrue(tenantId)).thenReturn(List.of(rule));

        RecallPatientQueryRepository.RecallCandidate candidate = mock(RecallPatientQueryRepository.RecallCandidate.class);
        UUID mockPatientId = UUID.randomUUID();
        when(candidate.getPatientId()).thenReturn(mockPatientId);
        when(candidate.getFirstName()).thenReturn("John");
        when(candidate.getLastName()).thenReturn("Doe");
        when(candidate.getEmail()).thenReturn("john@test.com");

        when(recallQueryRepository.findCandidates(eq(tenantId), eq("H40.1"), eq(rule.getId()), any(LocalDate.class)))
                .thenReturn(List.of(candidate));

        recallEngine.executeDailyRecalls();

        verify(ruleEngine).createRecallNotification(eq(tenantId), eq(mockPatientId), eq("John"), eq("Doe"), eq("john@test.com"), eq(rule));
    }

    @Test
    @DisplayName("skip_futureAppointmentExists_shouldNotCreateNotification")
    void skip_futureAppointmentExists_shouldNotCreateNotification() {
        UUID tenantId = UUID.randomUUID();
        
        RecallProtocolEntity protocol = setupProtocol(tenantId);
        NotificationRuleEntity rule = setupRule(tenantId, protocol.getId());

        when(protocolRepository.findAll()).thenReturn(List.of(protocol));
        when(ruleRepository.findByTenantIdAndIsActiveTrue(tenantId)).thenReturn(List.of(rule));

        // Repository returns empty list because SQL filters out patients with future appointments
        when(recallQueryRepository.findCandidates(eq(tenantId), anyString(), eq(rule.getId()), any(LocalDate.class)))
                .thenReturn(List.of());

        recallEngine.executeDailyRecalls();

        verify(ruleEngine, never()).createRecallNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("skip_notificationSentWithin30Days_shouldNotCreateNotification")
    void skip_notificationSentWithin30Days_shouldNotCreateNotification() {
        UUID tenantId = UUID.randomUUID();
        
        RecallProtocolEntity protocol = setupProtocol(tenantId);
        NotificationRuleEntity rule = setupRule(tenantId, protocol.getId());

        when(protocolRepository.findAll()).thenReturn(List.of(protocol));
        when(ruleRepository.findByTenantIdAndIsActiveTrue(tenantId)).thenReturn(List.of(rule));

        // Repository returns empty list because SQL filters out patients who already received a notification in the last 30 days
        when(recallQueryRepository.findCandidates(eq(tenantId), anyString(), eq(rule.getId()), any(LocalDate.class)))
                .thenReturn(List.of());

        recallEngine.executeDailyRecalls();

        verify(ruleEngine, never()).createRecallNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("skip_diagnosisNotMatchingProtocol_shouldNotCreateNotification")
    void skip_diagnosisNotMatchingProtocol_shouldNotCreateNotification() {
        UUID tenantId = UUID.randomUUID();
        
        RecallProtocolEntity protocol = setupProtocol(tenantId);
        NotificationRuleEntity rule = setupRule(tenantId, protocol.getId());

        when(protocolRepository.findAll()).thenReturn(List.of(protocol));
        when(ruleRepository.findByTenantIdAndIsActiveTrue(tenantId)).thenReturn(List.of(rule));

        // Repository returns empty list because SQL filters out patients whose diagnosis does not match
        when(recallQueryRepository.findCandidates(eq(tenantId), anyString(), eq(rule.getId()), any(LocalDate.class)))
                .thenReturn(List.of());

        recallEngine.executeDailyRecalls();

        verify(ruleEngine, never()).createRecallNotification(any(), any(), any(), any(), any(), any());
    }

    private RecallProtocolEntity setupProtocol(UUID tenantId) {
        RecallProtocolEntity protocol = new RecallProtocolEntity();
        protocol.setId(UUID.randomUUID());
        setTenantId(protocol, tenantId);
        protocol.setIcd10Code("H40.1");
        protocol.setRecallIntervalMonths(6);
        protocol.setIsActive(true);
        return protocol;
    }

    private NotificationRuleEntity setupRule(UUID tenantId, UUID protocolId) {
        NotificationRuleEntity rule = new NotificationRuleEntity();
        rule.setId(UUID.randomUUID());
        setTenantId(rule, tenantId);
        rule.setIsActive(true);
        rule.setConfigData("""
            {
                "triggerType": "RECALL",
                "recallProtocolId": "%s"
            }
        """.formatted(protocolId));
        return rule;
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
