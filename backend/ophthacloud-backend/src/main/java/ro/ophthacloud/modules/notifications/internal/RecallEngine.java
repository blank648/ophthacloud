package ro.ophthacloud.modules.notifications.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.shared.tenant.TenantContext;
import ro.ophthacloud.modules.notifications.internal.RecallPatientQueryRepository.RecallCandidate;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Automates the patient recall process based on medical history.
 * Runs daily at 03:00 to evaluate Recall Protocols and create PENDING notifications.
 * Per GUIDE_06 §7.4.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecallEngine {

    private final RecallProtocolRepository protocolRepository;
    private final NotificationRuleRepository ruleRepository;
    private final RecallPatientQueryRepository recallQueryRepository;
    private final NotificationRuleEngine ruleEngine;
    private final ObjectMapper objectMapper;

    // Runs daily at 03:00
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void executeDailyRecalls() {
        log.info("Starting daily RecallEngine execution...");
        // Since this is a background job without an HTTP request context, 
        // we normally need to iterate over all active tenants.
        // Assuming we have a way to list tenants or we query across tenants:
        // Wait! The specifications state that operations must run per tenant.
        // Actually, NotificationRuleEngine also needs a TenantContext if it creates logs.
        // For Phase 1, we can just log a warning if tenant context is missing.
        // BUT we need a way to fetch tenants. Let's just execute a native query for distinct tenants from active protocols,
        // then set the context and process.
        
        // Wait, since we are constrained to the module, we can just fetch all active protocols 
        // (which implicitly have tenant_id) and group them by tenant.
        List<RecallProtocolEntity> activeProtocols = protocolRepository.findAll().stream()
                .filter(RecallProtocolEntity::getIsActive)
                .toList();

        Map<UUID, List<RecallProtocolEntity>> protocolsByTenant = new HashMap<>();
        for (RecallProtocolEntity p : activeProtocols) {
            protocolsByTenant.computeIfAbsent(p.getTenantId(), k -> new ArrayList<>()).add(p);
        }

        for (Map.Entry<UUID, List<RecallProtocolEntity>> entry : protocolsByTenant.entrySet()) {
            UUID tenantId = entry.getKey();
            TenantContext.set(tenantId);
            try {
                processTenantProtocols(tenantId, entry.getValue());
            } catch (Exception e) {
                log.error("Error processing recalls for tenant {}", tenantId, e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void processTenantProtocols(UUID tenantId, List<RecallProtocolEntity> protocols) {
        log.info("Processing {} recall protocols for tenant {}", protocols.size(), tenantId);

        // Find all active RECALL rules for this tenant
        List<NotificationRuleEntity> recallRules = ruleRepository.findByTenantIdAndIsActiveTrue(tenantId).stream()
                .filter(r -> {
                    Map<String, Object> config = parseConfig(r);
                    return config != null && "RECALL".equals(config.get("triggerType"));
                })
                .toList();

        if (recallRules.isEmpty()) {
            log.debug("No active RECALL rules for tenant {}", tenantId);
            return;
        }

        for (RecallProtocolEntity protocol : protocols) {
            // Find rule that is linked to this protocol
            NotificationRuleEntity matchingRule = recallRules.stream()
                    .filter(r -> {
                        Map<String, Object> config = parseConfig(r);
                        return config != null && protocol.getId().toString().equals(config.get("recallProtocolId"));
                    })
                    .findFirst()
                    .orElse(null);

            if (matchingRule == null) {
                log.debug("No active rule found linking to protocol {}", protocol.getId());
                continue;
            }

            LocalDate cutoffDate = LocalDate.now().plusDays(7).minusMonths(protocol.getRecallIntervalMonths());
            List<RecallCandidate> candidates = recallQueryRepository.findCandidates(
                    tenantId, protocol.getIcd10Code(), matchingRule.getId(), cutoffDate);

            log.info("Protocol '{}' found {} candidates for recall.", protocol.getName(), candidates.size());

            for (RecallCandidate candidate : candidates) {
                ruleEngine.createRecallNotification(tenantId, candidate.getPatientId(), candidate.getFirstName(), candidate.getLastName(), candidate.getEmail(), matchingRule);
            }
        }
    }

    private Map<String, Object> parseConfig(NotificationRuleEntity rule) {
        try {
            return objectMapper.readValue(rule.getConfigData(), new TypeReference<HashMap<String, Object>>() {});
        } catch (Exception ex) {
            log.error("Failed to parse config_data for rule {}: {}", rule.getId(), ex.getMessage());
            return null;
        }
    }
}
