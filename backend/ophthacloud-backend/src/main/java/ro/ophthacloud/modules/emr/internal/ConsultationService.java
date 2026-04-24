package ro.ophthacloud.modules.emr.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.appointments.internal.AppointmentRepository;
import ro.ophthacloud.modules.emr.dto.*;
import ro.ophthacloud.modules.emr.event.ConsultationSignedEvent;
import ro.ophthacloud.shared.audit.AuditEntry;
import ro.ophthacloud.shared.audit.AuditLogService;
import ro.ophthacloud.shared.security.OphthaPrincipal;
import ro.ophthacloud.shared.security.SecurityUtils;
import ro.ophthacloud.shared.tenant.TenantContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service implementing all EMR consultation business logic.
 * All public methods are delegates of {@link ro.ophthacloud.modules.emr.EmrFacade}.
 * <p>
 * Business rules implemented:
 * <ul>
 *   <li>Create consultation + 9 section rows atomically (GUIDE_06 §3.1)</li>
 *   <li>SEQ auto-computation for Section A (GUIDE_06 §3.2)</li>
 *   <li>Bitmask OR for section completion (GUIDE_06 §3.5)</li>
 *   <li>Signing pre-conditions: Section G complete, DRAFT/IN_PROGRESS status, emr.SIGN permission (GUIDE_06 §3.6)</li>
 *   <li>Template apply-merge: no overwrite of non-null fields (GUIDE_06 §3.8)</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ConsultationService {

    // Ordered list of the 9 section codes A–I
    private static final List<String> SECTION_CODES =
            List.of("A", "B", "C", "D", "E", "F", "G", "H", "I");

    private final ConsultationRepository        consultationRepository;
    private final ConsultationSectionRepository sectionRepository;
    private final ClinicalTemplateRepository    templateRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final SectionDataValidator          sectionDataValidator;
    private final SeqCalculator                 seqCalculator;
    private final AuditLogService               auditLogService;
    private final ApplicationEventPublisher     eventPublisher;
    private final ObjectMapper                  objectMapper;

    // ── Create ───────────────────────────────────────────────────────────────

    /**
     * Creates the consultation record plus exactly 9 empty section rows (A–I) in one @Transactional call.
     * Sets appointments.consultation_id if appointmentId provided.
     * Per GUIDE_04 §4.1 and OC-018 acceptance criterion 1–2.
     */
    @Transactional
    public ConsultationDto createConsultation(CreateConsultationRequest request) {
        OphthaPrincipal principal = SecurityUtils.currentPrincipal();
        String staffId = principal.staffId() != null ? principal.staffId() : principal.keycloakUserId();

        ConsultationEntity consultation = ConsultationEntity.builder()
                .patientId(request.patientId())
                .appointmentId(request.appointmentId())
                .doctorId(UUID.fromString(staffId))
                .doctorName("Dr. " + principal.staffRole())
                .status(ConsultationStatus.DRAFT)
                .consultationDate(request.consultationDate() != null ? request.consultationDate() : LocalDate.now())
                .chiefComplaint(request.chiefComplaint())
                .sectionsCompleted((short) 0)
                .build();

        ConsultationEntity saved = consultationRepository.save(consultation);
        log.info("createConsultation: id={}, patient={}, doctor={}", saved.getId(), saved.getPatientId(), saved.getDoctorId());

        // Create empty section rows for A through I
        List<ConsultationSectionEntity> sections = SECTION_CODES.stream()
                .map(code -> ConsultationSectionEntity.builder()
                        .consultationId(saved.getId())
                        .sectionCode(code)
                        .sectionData("{}")
                        .isCompleted(false)
                        .build())
                .toList();
        List<ConsultationSectionEntity> savedSections = sectionRepository.saveAll(sections);

        // Link appointment if provided — raw JDBC to avoid cross-module entity dependency
        if (saved.getAppointmentId() != null) {
            jdbcTemplate.update(
                    "UPDATE appointments SET consultation_id = ? WHERE id = ?",
                    saved.getId(), saved.getAppointmentId());
            log.debug("createConsultation: linked appointment {} → consultation {}", saved.getAppointmentId(), saved.getId());
        }

        auditLogService.log(AuditEntry.builder()
                .action("CREATE")
                .entityType("Consultation")
                .entityId(saved.getId())
                .build());

        Map<String, ConsultationSectionDto> sectionMap = savedSections.stream()
                .collect(Collectors.toMap(
                        ConsultationSectionEntity::getSectionCode,
                        ConsultationSectionDto::from,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        return ConsultationDto.from(saved, sectionMap);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public ConsultationDto getConsultation(UUID id) {
        ConsultationEntity entity = requireConsultation(id);
        Map<String, ConsultationSectionDto> sections = buildSectionMap(id);

        auditLogService.log(AuditEntry.builder()
                .action("VIEW")
                .entityType("Consultation")
                .entityId(id)
                .build());

        return ConsultationDto.from(entity, sections);
    }

    public ConsultationSectionDto getSection(UUID consultationId, String sectionCode) {
        requireConsultation(consultationId); // tenant-check
        return sectionRepository.findByConsultationIdAndSectionCode(consultationId, sectionCode)
                .map(ConsultationSectionDto::from)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Section " + sectionCode + " not found for consultation " + consultationId));
    }

    public Page<ConsultationSummaryDto> listConsultations(UUID patientId, Pageable pageable) {
        UUID tenantId = TenantContext.get();
        if (patientId != null) {
            return consultationRepository.findByPatientIdAndTenantId(patientId, tenantId, pageable)
                    .map(ConsultationSummaryDto::from);
        }
        return consultationRepository.findByTenantId(tenantId, pageable)
                .map(ConsultationSummaryDto::from);
    }

    // ── Save section (auto-save) ──────────────────────────────────────────────

    /**
     * Auto-saves a consultation section. Validates JSON, auto-computes SEQ for Section A.
     * Returns 409 (ConsultationAlreadySignedException) if consultation is SIGNED.
     * Per GUIDE_04 §4.4 and GUIDE_06 §3.2.
     */
    @Transactional
    public ConsultationSectionDto saveSection(UUID consultationId, String sectionCode, SaveSectionRequest request) {
        ConsultationEntity consultation = requireConsultation(consultationId);

        // Pre-condition: not already signed
        if (consultation.getStatus() == ConsultationStatus.SIGNED) {
            throw new ConsultationAlreadySignedException(consultationId);
        }

        ConsultationSectionEntity section =
                sectionRepository.findByConsultationIdAndSectionCode(consultationId, sectionCode)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Section " + sectionCode + " not found for consultation " + consultationId));

        String sectionData = request.sectionData() != null ? request.sectionData() : "{}";

        // Validate clinical ranges for Section A
        sectionDataValidator.validateSectionData(sectionCode, sectionData);

        // Auto-compute SEQ for Section A (GUIDE_06 §3.2)
        if ("A".equals(sectionCode)) {
            sectionData = injectSeq(sectionData);
        }

        section.setSectionData(sectionData);
        if (request.isCompleted() != null && request.isCompleted()) {
            section.setCompleted(true);
            section.setCompletedAt(Instant.now());
        }

        ConsultationSectionEntity saved = sectionRepository.save(section);

        // Transition consultation to IN_PROGRESS if still DRAFT
        if (consultation.getStatus() == ConsultationStatus.DRAFT) {
            consultation.setStatus(ConsultationStatus.IN_PROGRESS);
            consultationRepository.save(consultation);
        }

        log.debug("saveSection: consultation={}, section={}", consultationId, sectionCode);
        return ConsultationSectionDto.from(saved);
    }

    // ── Mark Section Complete ─────────────────────────────────────────────────

    /**
     * Sets section is_completed = true and ORs the bitmask in consultation.sections_completed.
     * Per GUIDE_06 §3.5.
     */
    @Transactional
    public ConsultationDto markSectionComplete(UUID consultationId, String sectionCode) {
        ConsultationEntity consultation = requireConsultation(consultationId);

        if (consultation.getStatus() == ConsultationStatus.SIGNED) {
            throw new ConsultationAlreadySignedException(consultationId);
        }

        ConsultationSectionEntity section =
                sectionRepository.findByConsultationIdAndSectionCode(consultationId, sectionCode)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Section " + sectionCode + " not found for consultation " + consultationId));

        section.setCompleted(true);
        section.setCompletedAt(Instant.now());
        sectionRepository.save(section);

        // OR bitmask: A=0, B=1, ... I=8
        int sectionIndex = SECTION_CODES.indexOf(sectionCode);
        int bit = 1 << sectionIndex;
        short current = consultation.getSectionsCompleted() != null ? consultation.getSectionsCompleted() : 0;
        consultation.setSectionsCompleted((short) (current | bit));
        ConsultationEntity saved = consultationRepository.save(consultation);

        log.debug("markSectionComplete: consultation={}, section={}, bitmask={}", consultationId, sectionCode, saved.getSectionsCompleted());

        return ConsultationDto.from(saved, buildSectionMap(consultationId));
    }

    // ── Sign ─────────────────────────────────────────────────────────────────

    /**
     * Signs a consultation after validating all pre-conditions in order (GUIDE_06 §3.6):
     * 1. Status is DRAFT or IN_PROGRESS (not SIGNED) → 409
     * 2. Section G is completed → 422
     * 3. User has emr.SIGN permission → 403 (enforced via @PreAuthorize on controller)
     * 4. Consultation belongs to current tenant → 404 (enforced by Hibernate filter)
     */
    @Transactional
    public ConsultationDto signConsultation(UUID consultationId, SignConsultationRequest request) {
        if (!request.signatureConfirmation()) {
            throw new IllegalArgumentException("Signature confirmation must be true.");
        }

        ConsultationEntity consultation = requireConsultation(consultationId);

        // Pre-condition 1: not already signed
        if (consultation.getStatus() == ConsultationStatus.SIGNED) {
            throw new ConsultationAlreadySignedException(consultationId);
        }

        // Pre-condition 2: Section G (Diagnostic) must be completed
        ConsultationSectionEntity sectionG =
                sectionRepository.findByConsultationIdAndSectionCode(consultationId, "G")
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Section G not found for consultation " + consultationId));
        if (!sectionG.isCompleted()) {
            throw new IllegalStateException(
                    "Secțiunea G (Diagnostic) trebuie completată înainte de semnare.");
        }

        OphthaPrincipal principal = SecurityUtils.currentPrincipal();
        String staffId = principal.staffId() != null ? principal.staffId() : principal.keycloakUserId();
        Instant now = Instant.now();

        consultation.setStatus(ConsultationStatus.SIGNED);
        consultation.setSignedAt(now);
        consultation.setSignedById(UUID.fromString(staffId));
        ConsultationEntity saved = consultationRepository.save(consultation);

        log.info("signConsultation: id={}, signedBy={}", consultationId, staffId);

        auditLogService.log(AuditEntry.builder()
                .action("SIGN")
                .entityType("Consultation")
                .entityId(consultationId)
                .changedField("status", ConsultationStatus.IN_PROGRESS.name(), ConsultationStatus.SIGNED.name())
                .build());

        eventPublisher.publishEvent(new ConsultationSignedEvent(
                saved.getId(),
                saved.getPatientId(),
                saved.getTenantId(),
                saved.getSignedById(),
                saved.getDoctorName(),
                now
        ));

        return ConsultationDto.from(saved, buildSectionMap(consultationId));
    }

    // ── Template CRUD ─────────────────────────────────────────────────────────

    public List<ClinicalTemplateDto> listTemplates(String sectionCode) {
        List<ClinicalTemplateEntity> templates;
        if (sectionCode != null) {
            templates = templateRepository.findBySectionCode(sectionCode);
        } else {
            templates = templateRepository.findAll();
        }
        return templates.stream().map(ClinicalTemplateDto::from).toList();
    }

    @Transactional
    public ClinicalTemplateDto createTemplate(CreateTemplateRequest request) {
        OphthaPrincipal principal = SecurityUtils.currentPrincipal();

        String staffIdForTemplate = principal.staffId() != null ? principal.staffId() : principal.keycloakUserId();

        ClinicalTemplateEntity entity = ClinicalTemplateEntity.builder()
                .name(request.name())
                .sectionCode(request.sectionCode())
                .category(request.category())
                .isGlobal(request.isGlobal())
                .templateData(request.templateData())
                .createdById(UUID.fromString(staffIdForTemplate))
                .isActive(true)
                .build();

        ClinicalTemplateEntity saved = templateRepository.save(entity);
        log.info("createTemplate: id={}, section={}", saved.getId(), saved.getSectionCode());
        return ClinicalTemplateDto.from(saved);
    }

    @Transactional
    public ClinicalTemplateDto updateTemplate(UUID templateId, CreateTemplateRequest request) {
        ClinicalTemplateEntity entity = templateRepository.findById(templateId)
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        entity.setName(request.name());
        entity.setSectionCode(request.sectionCode());
        entity.setCategory(request.category());
        entity.setIsGlobal(request.isGlobal());
        entity.setTemplateData(request.templateData());

        ClinicalTemplateEntity saved = templateRepository.save(entity);
        return ClinicalTemplateDto.from(saved);
    }

    @Transactional
    public void deleteTemplate(UUID templateId) {
        ClinicalTemplateEntity entity = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        entity.setIsActive(false);
        templateRepository.save(entity);
    }

    // ── Apply Template ────────────────────────────────────────────────────────

    /**
     * Applies a clinical template to a section using merge semantics:
     * template fields fill in only where the section's current value is null.
     * Existing non-null fields are NOT overwritten.
     * Per GUIDE_06 §3.8.
     */
    @Transactional
    public ConsultationSectionDto applyTemplate(UUID consultationId, String sectionCode, UUID templateId) {
        ConsultationEntity consultation = requireConsultation(consultationId);
        if (consultation.getStatus() == ConsultationStatus.SIGNED) {
            throw new ConsultationAlreadySignedException(consultationId);
        }

        ConsultationSectionEntity section =
                sectionRepository.findByConsultationIdAndSectionCode(consultationId, sectionCode)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Section " + sectionCode + " not found for consultation " + consultationId));

        ClinicalTemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        String merged = mergeJsonData(section.getSectionData(), template.getTemplateData());
        section.setSectionData(merged);
        ConsultationSectionEntity saved = sectionRepository.save(section);

        log.debug("applyTemplate: consultation={}, section={}, template={}", consultationId, sectionCode, templateId);
        return ConsultationSectionDto.from(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ConsultationEntity requireConsultation(UUID id) {
        UUID tenantId = TenantContext.get();
        return consultationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ConsultationNotFoundException(id));
    }

    private Map<String, ConsultationSectionDto> buildSectionMap(UUID consultationId) {
        List<ConsultationSectionEntity> sections =
                sectionRepository.findAllByConsultationId(consultationId);
        return sections.stream()
                .collect(Collectors.toMap(
                        ConsultationSectionEntity::getSectionCode,
                        ConsultationSectionDto::from,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * Parses the Section A JSON data and injects computed SEQ values into od.seq and os.seq.
     * Per GUIDE_06 §3.2: SEQ = Sph + Cyl/2, rounded to 0.25D.
     */
    private String injectSeq(String sectionDataJson) {
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(sectionDataJson);

            injectEyeSeq(root, "od");
            injectEyeSeq(root, "os");

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("SEQ injection failed, returning original section data: {}", e.getMessage());
            return sectionDataJson;
        }
    }

    private void injectEyeSeq(ObjectNode root, String eye) {
        JsonNode eyeNode = root.path(eye);
        if (eyeNode.isMissingNode() || eyeNode.isNull() || !eyeNode.isObject()) {
            return;
        }
        ObjectNode eyeObj = (ObjectNode) eyeNode;

        Double sph = eyeObj.hasNonNull("sph") ? eyeObj.get("sph").asDouble() : null;
        Double cyl = eyeObj.hasNonNull("cyl") ? eyeObj.get("cyl").asDouble() : null;

        Double seq = seqCalculator.compute(sph, cyl);
        if (seq != null) {
            eyeObj.put("seq", seq);
        }
    }

    /**
     * Merges template fields into current section data — only fills null/missing fields.
     * Existing non-null values in current data take priority (GUIDE_06 §3.8).
     */
    private String mergeJsonData(String currentJson, String templateJson) {
        try {
            ObjectNode current  = (ObjectNode) objectMapper.readTree(
                    currentJson != null && !currentJson.isBlank() ? currentJson : "{}");
            JsonNode template = objectMapper.readTree(
                    templateJson != null && !templateJson.isBlank() ? templateJson : "{}");

            template.properties().forEach(entry -> {
                String key = entry.getKey();
                JsonNode templateValue = entry.getValue();
                // Only set if current field is missing or null
                if (!current.has(key) || current.get(key).isNull()) {
                    current.set(key, templateValue);
                }
            });

            return objectMapper.writeValueAsString(current);
        } catch (Exception e) {
            log.warn("Template merge failed, returning original section data: {}", e.getMessage());
            return currentJson;
        }
    }
}
