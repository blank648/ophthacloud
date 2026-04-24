package ro.ophthacloud.modules.emr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.ophthacloud.infrastructure.persistence.BaseEntity;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;
import ro.ophthacloud.modules.emr.dto.*;
import ro.ophthacloud.modules.emr.event.ConsultationSignedEvent;
import ro.ophthacloud.modules.emr.internal.*;
import ro.ophthacloud.shared.audit.AuditLogService;
import ro.ophthacloud.shared.security.ModulePermissions;
import ro.ophthacloud.shared.security.OphthaClinicalAuthenticationToken;
import ro.ophthacloud.shared.security.OphthaPrincipal;
import ro.ophthacloud.shared.tenant.TenantContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmrFacade} (via {@link ConsultationService}).
 * <p>
 * No Spring context — pure Mockito. Covers OC-020 acceptance criteria (≥ 8 tests):
 * create produces 9 sections, bitmask OR, sign blocked when G incomplete (422),
 * sign blocked when already SIGNED (409), save blocked when SIGNED (409),
 * template merge semantics, ConsultationSignedEvent published.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmrFacade (via ConsultationService)")
class EmrFacadeTest {

    @Mock private ConsultationRepository        consultationRepository;
    @Mock private ConsultationSectionRepository sectionRepository;
    @Mock private ClinicalTemplateRepository    templateRepository;
    @Mock private JdbcTemplate                  jdbcTemplate;
    @Mock private SectionDataValidator          sectionDataValidator;
    @Mock private SeqCalculator                 seqCalculator;
    @Mock private AuditLogService               auditLogService;
    @Mock private ApplicationEventPublisher     eventPublisher;

    private ConsultationService service;
    private EmrFacade           facade;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID STAFF_ID   = UUID.randomUUID();
    private static final UUID CONSULT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUpContext() {
        TenantContext.set(TENANT_ID);

        OphthaPrincipal principal = new OphthaPrincipal(
                STAFF_ID.toString(),
                TENANT_ID.toString(),
                STAFF_ID.toString(),
                "DOCTOR",
                Map.of("emr", new ModulePermissions(true, true, true, true, true, false))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new OphthaClinicalAuthenticationToken(principal, null, List.of()));

        ObjectMapper objectMapper = JsonMapper.builder().build();
        service = new ConsultationService(
                consultationRepository,
                sectionRepository,
                templateRepository,
                jdbcTemplate,
                sectionDataValidator,
                seqCalculator,
                auditLogService,
                eventPublisher,
                objectMapper
        );
        facade = new EmrFacade(service);

        doNothing().when(auditLogService).log(any());
        doNothing().when(sectionDataValidator).validateSectionData(any(), any());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ── Helper — creates a mock ConsultationEntity ────────────────────────────

    private ConsultationEntity consultationEntity(ConsultationStatus status) {
        ConsultationEntity e = new ConsultationEntity();
        setId(e, CONSULT_ID);
        setTenantId(e, TENANT_ID);
        e.setPatientId(PATIENT_ID);
        e.setDoctorId(STAFF_ID);
        e.setDoctorName("Dr. Test");
        e.setStatus(status);
        e.setSectionsCompleted((short) 0);
        return e;
    }

    private ConsultationSectionEntity sectionEntity(String code, boolean completed) {
        ConsultationSectionEntity s = new ConsultationSectionEntity();
        setId(s, UUID.randomUUID());
        setTenantId(s, TENANT_ID);
        s.setConsultationId(CONSULT_ID);
        s.setSectionCode(code);
        s.setSectionData("{}");
        s.setCompleted(completed);
        return s;
    }

    // ── Test 1: createConsultation produces exactly 9 section rows ───────────

    @Test
    @DisplayName("createConsultation: should save exactly 9 sections (A–I) in one transaction")
    void createConsultation_shouldSaveExactly9Sections() {
        ConsultationEntity saved = consultationEntity(ConsultationStatus.DRAFT);

        when(consultationRepository.save(any())).thenReturn(saved);
        when(sectionRepository.saveAll(any())).thenAnswer(inv -> inv.<List<?>>getArgument(0));

        CreateConsultationRequest req = new CreateConsultationRequest(PATIENT_ID, null, null, "Chief complaint");
        facade.createConsultation(req);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ConsultationSectionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(sectionRepository).saveAll(captor.capture());

        List<ConsultationSectionEntity> savedSections = captor.getValue();
        assertThat(savedSections).hasSize(9);

        // Verify section codes are exactly A through I
        List<String> codes = savedSections.stream()
                .map(ConsultationSectionEntity::getSectionCode)
                .toList();
        assertThat(codes).containsExactly("A", "B", "C", "D", "E", "F", "G", "H", "I");
    }

    // ── Test 2: bitmask starts at 0 ──────────────────────────────────────────

    @Test
    @DisplayName("createConsultation: should initialize sectionsCompleted bitmask to 0")
    void createConsultation_shouldInitializeBitmaskToZero() {
        ConsultationEntity saved = consultationEntity(ConsultationStatus.DRAFT);

        when(consultationRepository.save(any())).thenReturn(saved);
        when(sectionRepository.saveAll(any())).thenAnswer(inv -> inv.<List<?>>getArgument(0));

        facade.createConsultation(new CreateConsultationRequest(PATIENT_ID, null, null, null));

        assertThat(saved.getSectionsCompleted()).isEqualTo((short) 0);
    }

    // ── Test 3: markSectionComplete ORs bitmask correctly ────────────────────
    //    A=bit0(1), G=bit6(64) → completing A then G → 65

    @Test
    @DisplayName("markSectionComplete: should OR bitmask correctly (A=1, G=64)")
    void markSectionComplete_shouldOrBitmaskCorrectly() {
        ConsultationEntity consultation = consultationEntity(ConsultationStatus.IN_PROGRESS);

        // First complete A (index 0 → bit 1)
        ConsultationSectionEntity sectionA = sectionEntity("A", false);
        when(consultationRepository.findByIdAndTenantId(CONSULT_ID, TENANT_ID)).thenReturn(Optional.of(consultation));
        when(sectionRepository.findByConsultationIdAndSectionCode(CONSULT_ID, "A")).thenReturn(Optional.of(sectionA));
        when(sectionRepository.save(sectionA)).thenReturn(sectionA);
        when(consultationRepository.save(consultation)).thenReturn(consultation);
        when(sectionRepository.findAllByConsultationId(CONSULT_ID)).thenReturn(List.of(sectionA));

        facade.markSectionComplete(CONSULT_ID, "A");
        assertThat(consultation.getSectionsCompleted()).isEqualTo((short) 1);

        // Then complete G (index 6 → bit 64)
        ConsultationSectionEntity sectionG = sectionEntity("G", false);
        when(sectionRepository.findByConsultationIdAndSectionCode(CONSULT_ID, "G")).thenReturn(Optional.of(sectionG));
        when(sectionRepository.save(sectionG)).thenReturn(sectionG);

        facade.markSectionComplete(CONSULT_ID, "G");
        assertThat(consultation.getSectionsCompleted()).isEqualTo((short) 65); // 1 | 64
    }

    // ── Test 4: signConsultation blocked when G not completed (422) ───────────

    @Test
    @DisplayName("signConsultation: should throw IllegalStateException when Section G is not completed")
    void signConsultation_shouldThrow_whenSectionGNotCompleted() {
        ConsultationEntity consultation = consultationEntity(ConsultationStatus.IN_PROGRESS);
        ConsultationSectionEntity sectionG = sectionEntity("G", false); // not completed

        when(consultationRepository.findByIdAndTenantId(CONSULT_ID, TENANT_ID)).thenReturn(Optional.of(consultation));
        when(sectionRepository.findByConsultationIdAndSectionCode(CONSULT_ID, "G")).thenReturn(Optional.of(sectionG));

        assertThatThrownBy(() -> facade.signConsultation(CONSULT_ID, new SignConsultationRequest(true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("G");
    }

    // ── Test 5: signConsultation blocked when already SIGNED (409) ───────────

    @Test
    @DisplayName("signConsultation: should throw ConsultationAlreadySignedException when already SIGNED")
    void signConsultation_shouldThrow_whenAlreadySigned() {
        ConsultationEntity consultation = consultationEntity(ConsultationStatus.SIGNED);
        when(consultationRepository.findByIdAndTenantId(CONSULT_ID, TENANT_ID)).thenReturn(Optional.of(consultation));

        assertThatThrownBy(() -> facade.signConsultation(CONSULT_ID, new SignConsultationRequest(true)))
                .isInstanceOf(ConsultationAlreadySignedException.class);
    }

    // ── Test 6: saveSection blocked when SIGNED (409) ────────────────────────

    @Test
    @DisplayName("saveSection: should throw ConsultationAlreadySignedException when consultation is SIGNED")
    void saveSection_shouldThrow_whenConsultationIsSigned() {
        ConsultationEntity consultation = consultationEntity(ConsultationStatus.SIGNED);
        when(consultationRepository.findByIdAndTenantId(CONSULT_ID, TENANT_ID)).thenReturn(Optional.of(consultation));

        assertThatThrownBy(() -> facade.saveSection(CONSULT_ID, "B", new SaveSectionRequest("{}", false)))
                .isInstanceOf(ConsultationAlreadySignedException.class);
    }

    // ── Test 7: signConsultation publishes ConsultationSignedEvent ────────────

    @Test
    @DisplayName("signConsultation: should publish ConsultationSignedEvent after successful signing")
    void signConsultation_shouldPublishConsultationSignedEvent() {
        ConsultationEntity consultation = consultationEntity(ConsultationStatus.IN_PROGRESS);
        ConsultationSectionEntity sectionG = sectionEntity("G", true); // completed

        when(consultationRepository.findByIdAndTenantId(CONSULT_ID, TENANT_ID)).thenReturn(Optional.of(consultation));
        when(sectionRepository.findByConsultationIdAndSectionCode(CONSULT_ID, "G")).thenReturn(Optional.of(sectionG));
        when(consultationRepository.save(consultation)).thenReturn(consultation);
        when(sectionRepository.findAllByConsultationId(CONSULT_ID)).thenReturn(List.of(sectionG));

        facade.signConsultation(CONSULT_ID, new SignConsultationRequest(true));

        ArgumentCaptor<ConsultationSignedEvent> captor = ArgumentCaptor.forClass(ConsultationSignedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        ConsultationSignedEvent event = captor.getValue();
        assertThat(event.consultationId()).isEqualTo(CONSULT_ID);
        assertThat(event.patientId()).isEqualTo(PATIENT_ID);
        assertThat(event.signedAt()).isNotNull();
    }

    // ── Test 8: signConsultation sets status=SIGNED, signedAt, signedById ─────

    @Test
    @DisplayName("signConsultation: should set status=SIGNED, signedAt and signedById")
    void signConsultation_shouldSetSignedFields() {
        ConsultationEntity consultation = consultationEntity(ConsultationStatus.IN_PROGRESS);
        ConsultationSectionEntity sectionG = sectionEntity("G", true);

        when(consultationRepository.findByIdAndTenantId(CONSULT_ID, TENANT_ID)).thenReturn(Optional.of(consultation));
        when(sectionRepository.findByConsultationIdAndSectionCode(CONSULT_ID, "G")).thenReturn(Optional.of(sectionG));
        when(consultationRepository.save(consultation)).thenReturn(consultation);
        when(sectionRepository.findAllByConsultationId(CONSULT_ID)).thenReturn(List.of(sectionG));

        facade.signConsultation(CONSULT_ID, new SignConsultationRequest(true));

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.SIGNED);
        assertThat(consultation.getSignedAt()).isNotNull();
        assertThat(consultation.getSignedById()).isEqualTo(STAFF_ID);
    }

    // ── Test 9: applyTemplate merges without overwriting non-null fields ───────

    @Test
    @DisplayName("applyTemplate: should merge template data but NOT overwrite existing non-null fields")
    void applyTemplate_shouldMergeTemplate_withoutOverwritingExistingFields() {
        ConsultationEntity consultation = consultationEntity(ConsultationStatus.IN_PROGRESS);
        ConsultationSectionEntity section = sectionEntity("B", false);
        section.setSectionData("{\"existing\":\"keep_me\"}");

        ClinicalTemplateEntity template = new ClinicalTemplateEntity();
        setId(template, UUID.randomUUID());
        setTenantId(template, TENANT_ID);
        template.setTemplateData("{\"existing\":\"overwrite_attempt\",\"new_field\":\"added\"}");

        UUID templateId = template.getId();

        when(consultationRepository.findByIdAndTenantId(CONSULT_ID, TENANT_ID)).thenReturn(Optional.of(consultation));
        when(sectionRepository.findByConsultationIdAndSectionCode(CONSULT_ID, "B")).thenReturn(Optional.of(section));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(sectionRepository.save(section)).thenReturn(section);

        ConsultationSectionDto result = facade.applyTemplate(CONSULT_ID, "B", templateId);

        // The existing field must NOT be overwritten; the new field must be present
        assertThat(result.sectionData()).contains("keep_me");
        assertThat(result.sectionData()).contains("new_field");
        assertThat(result.sectionData()).doesNotContain("overwrite_attempt");
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

    private void setId(Object entity, UUID id) {
        setField(entity, BaseEntity.class, "id", id);
    }

    private void setTenantId(Object entity, UUID tenantId) {
        setField(entity, TenantAwareEntity.class, "tenantId", tenantId);
    }

    private void setField(Object target, Class<?> declaringClass, String name, Object value) {
        try {
            Field f = declaringClass.getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field '" + name + "' via reflection", e);
        }
    }
}
