package ro.ophthacloud.modules.notifications.internal;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Evaluates tenant-specific {@link NotificationRuleEntity} records against an event trigger type,
 * and creates PENDING {@link NotificationLogEntity} entries for each matched rule.
 * <p>
 * Matching logic (GUIDE_06 §7.1–7.2):
 * <ol>
 *   <li>Load all active notification rules for the tenant</li>
 *   <li>For each rule, parse {@code config_data} JSONB to extract {@code trigger_type}</li>
 *   <li>If the rule's trigger type matches the incoming event type, render the template and
 *       create a PENDING log entry for each configured channel</li>
 *   <li>If no active rules match, the event is silently ignored</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRuleEngine {

    private final NotificationRuleRepository notificationRuleRepository;
    private final NotificationLogRepository  notificationLogRepository;
    private final TemplateRenderer           templateRenderer;
    private final ObjectMapper               objectMapper;

    /**
     * Evaluates all active notification rules for the given tenant and creates PENDING log
     * entries for any rule whose trigger type matches.
     *
     * @param tenantId         the tenant to evaluate rules for
     * @param triggerType      the event trigger type (e.g. {@code "APPOINTMENT_CONFIRMED"})
     * @param recipientAddress the email or phone address for the notification
     * @param patientId        the patient UUID (may be null for admin alerts like LOW_STOCK)
     * @param variables        Mustache template variables to interpolate
     */
    @Transactional
    public void evaluate(
            UUID tenantId,
            String triggerType,
            String recipientAddress,
            UUID patientId,
            Map<String, String> variables
    ) {
        List<NotificationRuleEntity> activeRules = notificationRuleRepository.findByIsActiveTrue();
        int created = 0;

        for (NotificationRuleEntity rule : activeRules) {
            // Rule must belong to this tenant
            if (!tenantId.equals(rule.getTenantId())) {
                continue;
            }

            Map<String, Object> config = parseConfig(rule);
            if (config == null) continue;

            String ruleTrigger = (String) config.get("trigger_type");
            if (!triggerType.equals(ruleTrigger)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            List<String> channels = (List<String>) config.getOrDefault("channels", List.of("EMAIL"));

            for (String channelStr : channels) {
                NotificationChannel channel;
                try {
                    channel = NotificationChannel.valueOf(channelStr);
                } catch (IllegalArgumentException ex) {
                    log.warn("Unknown channel '{}' in rule {}, skipping", channelStr, rule.getId());
                    continue;
                }

                String subjectTemplate = (String) config.getOrDefault("template_email_subject", "");
                String bodyTemplate    = (String) config.getOrDefault("template_email_body", "");

                String subject     = templateRenderer.render(subjectTemplate, variables);
                String bodyPreview = truncate(templateRenderer.render(bodyTemplate, variables), 512);

                NotificationLogEntity entry = new NotificationLogEntity();
                entry.setPatientId(patientId);
                entry.setRuleId(rule.getId());
                entry.setChannel(channel);
                entry.setStatus(NotificationStatus.PENDING);
                entry.setRecipientAddress(recipientAddress);
                entry.setSubject(subject);
                entry.setBodyPreview(bodyPreview);

                notificationLogRepository.save(entry);
                created++;
                log.debug("Created PENDING notification: rule={}, trigger={}, channel={}, recipient={}",
                        rule.getId(), triggerType, channel, recipientAddress);
            }
        }

        if (created == 0) {
            log.debug("No active rules matched trigger='{}' for tenant={}", triggerType, tenantId);
        } else {
            log.info("Created {} PENDING notification(s) for trigger='{}', tenant={}", created, triggerType, tenantId);
        }
    }

    /**
     * Specifically creates a PENDING recall notification bypassing the general event trigger matching,
     * since the RecallEngine explicitly matched the patient to a specific rule.
     */
    @Transactional
    public void createRecallNotification(
            UUID tenantId,
            UUID patientId,
            String patientFirstName,
            String patientLastName,
            String recipientAddress,
            NotificationRuleEntity rule
    ) {
        Map<String, Object> config = parseConfig(rule);
        if (config == null) return;

        Map<String, String> variables = new HashMap<>();
        variables.put("patientName", patientFirstName + " " + patientLastName);
        variables.put("patientFirstName", patientFirstName);

        @SuppressWarnings("unchecked")
        List<String> channels = (List<String>) config.getOrDefault("channels", List.of("EMAIL"));

        for (String channelStr : channels) {
            NotificationChannel channel;
            try {
                channel = NotificationChannel.valueOf(channelStr);
            } catch (IllegalArgumentException ex) {
                log.warn("Unknown channel '{}' in rule {}, skipping", channelStr, rule.getId());
                continue;
            }

            String subjectTemplate = (String) config.getOrDefault("template_email_subject", "");
            String bodyTemplate    = (String) config.getOrDefault("template_email_body", "");

            String subject     = templateRenderer.render(subjectTemplate, variables);
            String bodyPreview = truncate(templateRenderer.render(bodyTemplate, variables), 512);

            NotificationLogEntity entry = new NotificationLogEntity();
            entry.setPatientId(patientId);
            entry.setRuleId(rule.getId());
            entry.setChannel(channel);
            entry.setStatus(NotificationStatus.PENDING);
            entry.setRecipientAddress(recipientAddress);
            entry.setSubject(subject);
            entry.setBodyPreview(bodyPreview);

            notificationLogRepository.save(entry);
            log.debug("Created PENDING recall notification for patient {}", patientId);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, Object> parseConfig(NotificationRuleEntity rule) {
        try {
            return objectMapper.readValue(rule.getConfigData(), new TypeReference<HashMap<String, Object>>() {});
        } catch (Exception ex) {
            log.error("Failed to parse config_data for rule {}: {}", rule.getId(), ex.getMessage());
            return null;
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
