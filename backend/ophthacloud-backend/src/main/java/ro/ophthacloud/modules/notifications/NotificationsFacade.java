package ro.ophthacloud.modules.notifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.notifications.internal.NotificationLogRepository;
import ro.ophthacloud.modules.notifications.internal.NotificationRuleRepository;
import ro.ophthacloud.modules.notifications.internal.RecallProtocolRepository;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import java.util.UUID;

/**
 * The ONLY public API of the notifications module.
 * <p>
 * Handles:
 * <ul>
 *   <li>Notification rules configuration</li>
 *   <li>Notification log queries and audit</li>
 *   <li>Recall protocol management</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class NotificationsFacade {

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationRuleRepository notificationRuleRepository;
    private final RecallProtocolRepository recallProtocolRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    // ── Notification Rules ───────────────────────────────────────────────────

    public org.springframework.data.domain.Page<ro.ophthacloud.modules.notifications.dto.NotificationRuleDto> getNotificationRules(org.springframework.data.domain.Pageable pageable) {
        return notificationRuleRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional
    public ro.ophthacloud.modules.notifications.dto.NotificationRuleDto createRule(ro.ophthacloud.modules.notifications.dto.CreateNotificationRuleRequest request) {
        ro.ophthacloud.modules.notifications.internal.NotificationRuleEntity rule = new ro.ophthacloud.modules.notifications.internal.NotificationRuleEntity();
        rule.setName(request.name());
        try {
            rule.setConfigData(objectMapper.writeValueAsString(request.configData()));
        } catch (JacksonException e) {
            throw new RuntimeException("Invalid config data", e);
        }
        rule.setIsActive(request.isActive());
        return toDto(notificationRuleRepository.save(rule));
    }

    @Transactional
    public ro.ophthacloud.modules.notifications.dto.NotificationRuleDto updateRule(UUID id, ro.ophthacloud.modules.notifications.dto.CreateNotificationRuleRequest request) {
        ro.ophthacloud.modules.notifications.internal.NotificationRuleEntity rule = notificationRuleRepository.findById(id).orElseThrow();
        rule.setName(request.name());
        try {
            rule.setConfigData(objectMapper.writeValueAsString(request.configData()));
        } catch (JacksonException e) {
            throw new RuntimeException("Invalid config data", e);
        }
        rule.setIsActive(request.isActive());
        return toDto(notificationRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(UUID id) {
        notificationRuleRepository.deleteById(id);
    }

    @Transactional
    public ro.ophthacloud.modules.notifications.dto.NotificationRuleDto toggleRule(UUID id) {
        ro.ophthacloud.modules.notifications.internal.NotificationRuleEntity rule = notificationRuleRepository.findById(id).orElseThrow();
        rule.setIsActive(!rule.getIsActive());
        return toDto(notificationRuleRepository.save(rule));
    }

    private ro.ophthacloud.modules.notifications.dto.NotificationRuleDto toDto(ro.ophthacloud.modules.notifications.internal.NotificationRuleEntity entity) {
        java.util.Map<String, Object> config = null;
        try {
            config = objectMapper.readValue(entity.getConfigData(), new tools.jackson.core.type.TypeReference<java.util.HashMap<String, Object>>() {});
        } catch (Exception ignored) {}
        return new ro.ophthacloud.modules.notifications.dto.NotificationRuleDto(
                entity.getId(), entity.getName(), config, entity.getIsActive()
        );
    }

    // ── Notification Log ─────────────────────────────────────────────────────

    public org.springframework.data.domain.Page<ro.ophthacloud.modules.notifications.dto.NotificationLogDto> listNotificationLog(UUID patientId, ro.ophthacloud.modules.notifications.internal.NotificationStatus status, org.springframework.data.domain.Pageable pageable) {
        UUID tenantId = ro.ophthacloud.shared.tenant.TenantContext.require();
        org.springframework.data.domain.Page<ro.ophthacloud.modules.notifications.internal.NotificationLogEntity> page;
        
        if (patientId != null) {
            page = notificationLogRepository.findByTenantIdAndPatientId(tenantId, patientId, pageable);
        } else if (status != null) {
            page = notificationLogRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else {
            page = notificationLogRepository.findByTenantId(tenantId, pageable);
        }
        
        return page.map(l -> {
            String patientName = "Pacient Necunoscut";
            if (l.getPatientId() != null) {
                try {
                    patientName = jdbcTemplate.queryForObject(
                        "SELECT first_name || ' ' || last_name FROM patients WHERE id = ?",
                        String.class,
                        l.getPatientId()
                    );
                } catch (Exception ignored) {}
            }
            return new ro.ophthacloud.modules.notifications.dto.NotificationLogDto(
                l.getId(), patientName, l.getChannel().name(), l.getStatus().name(),
                l.getRecipientAddress(), l.getSubject(), l.getBodyPreview(), l.getSentAt(), l.getExternalMessageId()
            );
        });
    }

    // ── Recall Protocols ─────────────────────────────────────────────────────

    public org.springframework.data.domain.Page<ro.ophthacloud.modules.notifications.dto.RecallProtocolDto> listRecallProtocols(org.springframework.data.domain.Pageable pageable) {
        return recallProtocolRepository.findAll(pageable).map(p -> new ro.ophthacloud.modules.notifications.dto.RecallProtocolDto(
                p.getId(), p.getName(), p.getIcd10Code(), p.getRecallIntervalMonths(), p.getDescription(), p.getIsActive()
        ));
    }

    @Transactional
    public ro.ophthacloud.modules.notifications.dto.RecallProtocolDto createRecallProtocol(ro.ophthacloud.modules.notifications.dto.CreateRecallProtocolRequest request) {
        ro.ophthacloud.modules.notifications.internal.RecallProtocolEntity protocol = new ro.ophthacloud.modules.notifications.internal.RecallProtocolEntity();
        protocol.setName(request.name());
        protocol.setIcd10Code(request.icd10Code());
        protocol.setRecallIntervalMonths(request.recallIntervalMonths());
        protocol.setDescription(request.description());
        protocol.setIsActive(request.isActive());
        protocol = recallProtocolRepository.save(protocol);
        return new ro.ophthacloud.modules.notifications.dto.RecallProtocolDto(
                protocol.getId(), protocol.getName(), protocol.getIcd10Code(), protocol.getRecallIntervalMonths(), protocol.getDescription(), protocol.getIsActive()
        );
    }

    @Transactional
    public ro.ophthacloud.modules.notifications.dto.RecallProtocolDto updateRecallProtocol(UUID id, ro.ophthacloud.modules.notifications.dto.CreateRecallProtocolRequest request) {
        ro.ophthacloud.modules.notifications.internal.RecallProtocolEntity protocol = recallProtocolRepository.findById(id).orElseThrow();
        protocol.setName(request.name());
        protocol.setIcd10Code(request.icd10Code());
        protocol.setRecallIntervalMonths(request.recallIntervalMonths());
        protocol.setDescription(request.description());
        protocol.setIsActive(request.isActive());
        protocol = recallProtocolRepository.save(protocol);
        return new ro.ophthacloud.modules.notifications.dto.RecallProtocolDto(
                protocol.getId(), protocol.getName(), protocol.getIcd10Code(), protocol.getRecallIntervalMonths(), protocol.getDescription(), protocol.getIsActive()
        );
    }

    @Transactional
    public void deleteRecallProtocol(UUID id) {
        recallProtocolRepository.deleteById(id);
    }
}
