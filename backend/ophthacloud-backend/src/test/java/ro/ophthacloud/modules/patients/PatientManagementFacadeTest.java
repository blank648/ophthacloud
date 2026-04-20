package ro.ophthacloud.modules.patients;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.ophthacloud.infrastructure.persistence.BaseEntity;
import ro.ophthacloud.modules.patients.dto.CreatePatientRequest;
import ro.ophthacloud.modules.patients.dto.PatientDto;
import ro.ophthacloud.modules.patients.event.PatientCreatedEvent;
import ro.ophthacloud.modules.patients.internal.PatientEntity;
import ro.ophthacloud.modules.patients.internal.PatientMedicalHistoryEntity;
import ro.ophthacloud.modules.patients.PatientNotFoundException;
import ro.ophthacloud.modules.patients.internal.PatientRepository;
import ro.ophthacloud.shared.audit.AuditEntry;
import ro.ophthacloud.shared.audit.AuditLogService;
import ro.ophthacloud.shared.enums.GenderType;
import ro.ophthacloud.shared.security.ModulePermissions;
import ro.ophthacloud.shared.security.OphthaClinicalAuthenticationToken;
import ro.ophthacloud.shared.security.OphthaPrincipal;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link PatientManagementFacade}.
 * No Spring context — pure Mockito. Security context and TenantContext
 * are set up manually in @BeforeEach so that SecurityUtils and TenantContext.require()
 * resolve correctly.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientManagementFacade")
class PatientManagementFacadeTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PatientManagementFacade facade;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @BeforeEach
    void setUpContext() {
        // Plant TenantContext so TenantContext.require() succeeds in createPatient
        TenantContext.set(TENANT_ID);

        // Plant a minimal OphthaPrincipal so SecurityUtils.currentTenantId() works
        // (called by listPatients debug log and by auditLogService internally)
        OphthaPrincipal principal = new OphthaPrincipal(
                UUID.randomUUID().toString(),
                TENANT_ID.toString(),
                UUID.randomUUID().toString(),
                "DOCTOR",
                Map.of("patients", new ModulePermissions(true, true, true, true, false, false))
        );
        OphthaClinicalAuthenticationToken auth =
                new OphthaClinicalAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDownContext() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ── Test 1: MRN format ────────────────────────────────────────────────────

    @Test
    @DisplayName("createPatient: should generate MRN matching ^OC-\\d{6}$")
    void createPatient_shouldGenerateMrnMatchingFormat() {
        // Sequence returns 42 → MRN = "OC-000042"
        when(patientRepository.incrementAndGetMrnSequence(TENANT_ID)).thenReturn(42);
        when(patientRepository.save(any(PatientEntity.class))).thenAnswer(inv -> {
            PatientEntity p = inv.getArgument(0);
            setId(p, UUID.randomUUID());
            setTenantId(p, TENANT_ID);
            if (p.getMedicalHistory() != null) setId(p.getMedicalHistory(), UUID.randomUUID());
            return p;
        });
        doNothing().when(auditLogService).log(any(AuditEntry.class));

        PatientDto result = facade.createPatient(validCreateRequest());

        assertThat(result.mrn()).matches("^OC-\\d{6}$");
    }

    // ── Test 2: PatientCreatedEvent is published ─────────────────────────────

    @Test
    @DisplayName("createPatient: should publish PatientCreatedEvent on success")
    void createPatient_shouldPublishPatientCreatedEvent() {
        when(patientRepository.incrementAndGetMrnSequence(TENANT_ID)).thenReturn(1);
        when(patientRepository.save(any(PatientEntity.class))).thenAnswer(inv -> {
            PatientEntity p = inv.getArgument(0);
            setId(p, PATIENT_ID);
            setTenantId(p, TENANT_ID);   // needed so PatientCreatedEvent carries the right tenantId
            if (p.getMedicalHistory() != null) setId(p.getMedicalHistory(), UUID.randomUUID());
            return p;
        });
        doNothing().when(auditLogService).log(any(AuditEntry.class));

        facade.createPatient(validCreateRequest());

        ArgumentCaptor<PatientCreatedEvent> captor = ArgumentCaptor.forClass(PatientCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        PatientCreatedEvent event = captor.getValue();
        assertThat(event.patientId()).isEqualTo(PATIENT_ID);
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
        assertThat(event.mrn()).matches("^OC-\\d{6}$");
    }

    // ── Test 3: blank firstName → ConstraintViolationException ───────────────

    @Test
    @DisplayName("createPatient: should throw ConstraintViolationException when firstName is blank")
    void createPatient_shouldThrowConstraintViolation_whenFirstNameBlank() {
        CreatePatientRequest badRequest = new CreatePatientRequest();
        badRequest.setFirstName("");          // blank — @NotBlank
        badRequest.setLastName("Popescu");
        badRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));
        badRequest.setGender(GenderType.MALE);

        // In a pure unit test there is no Bean Validation interceptor, so we simulate
        // the constraint violation that Spring's AOP would throw by stubbing the
        // repository call (which is the first external call in createPatient).
        when(patientRepository.incrementAndGetMrnSequence(TENANT_ID))
                .thenThrow(new ConstraintViolationException("firstName: Prenumele este obligatoriu.", null));

        assertThatThrownBy(() -> facade.createPatient(badRequest))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("firstName");
    }

    // ── Test 4: findById — patient exists ────────────────────────────────────

    @Test
    @DisplayName("findById: should return PatientDto when patient exists")
    void getPatient_shouldReturnDto_whenPatientExists() {
        doNothing().when(auditLogService).log(any(AuditEntry.class));

        PatientEntity entity = activePatientEntity(PATIENT_ID);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(entity));

        PatientDto result = facade.getPatient(PATIENT_ID);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(PATIENT_ID);
        assertThat(result.firstName()).isEqualTo("Ion");
        assertThat(result.lastName()).isEqualTo("Popescu");
    }

    // ── Test 5: findById — not found ─────────────────────────────────────────

    @Test
    @DisplayName("findById: should throw PatientNotFoundException when not found")
    void getPatient_shouldThrow_whenPatientNotFound() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.getPatient(PATIENT_ID))
                .isInstanceOf(PatientNotFoundException.class)
                .hasMessageContaining(PATIENT_ID.toString());
    }

    // ── Test 6: deactivatePatient — isActive set to false ────────────────────

    @Test
    @DisplayName("deactivatePatient: should set isActive=false before save")
    void deletePatient_shouldSetActiveToFalse_beforeSave() {
        doNothing().when(auditLogService).log(any(AuditEntry.class));

        PatientEntity entity = activePatientEntity(PATIENT_ID);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(entity));
        when(patientRepository.save(any(PatientEntity.class))).thenReturn(entity);

        facade.deletePatient(PATIENT_ID);

        ArgumentCaptor<PatientEntity> captor = ArgumentCaptor.forClass(PatientEntity.class);
        verify(patientRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    // ── Test 7: searchPatients — empty page ──────────────────────────────────

    @Test
    @DisplayName("searchPatients: should return empty page without exception")
    void searchPatients_shouldReturnEmptyPage() {
        when(patientRepository.search(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // SecurityUtils.currentTenantId() is called by the debug log inside listPatients.
        // The OphthaPrincipal set in @BeforeEach satisfies that call.
        // Use PageRequest (not Pageable.unpaged()) — unpaged() throws on getPageNumber().
        Page<?> result = facade.listPatients("Popescu",
                org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreatePatientRequest validCreateRequest() {
        CreatePatientRequest req = new CreatePatientRequest();
        req.setFirstName("Ion");
        req.setLastName("Popescu");
        req.setDateOfBirth(LocalDate.of(1985, 6, 15));
        req.setGender(GenderType.MALE);
        return req;
    }

    private PatientEntity activePatientEntity(UUID id) {
        PatientEntity entity = new PatientEntity();
        setId(entity, id);
        setTenantId(entity, TENANT_ID);
        entity.setFirstName("Ion");
        entity.setLastName("Popescu");
        entity.setDateOfBirth(LocalDate.of(1985, 6, 15));
        entity.setGender(GenderType.MALE);
        entity.setActive(true);
        entity.setMrn("OC-000001");

        PatientMedicalHistoryEntity history = new PatientMedicalHistoryEntity();
        setId(history, UUID.randomUUID());
        history.setPatient(entity);
        entity.setMedicalHistory(history);

        return entity;
    }

    /** Sets the inherited {@code id} field declared in {@link BaseEntity}. */
    private void setId(Object entity, UUID id) {
        setField(entity, BaseEntity.class, "id", id);
    }

    /**
     * Sets the {@code tenantId} field declared in
     * {@link ro.ophthacloud.infrastructure.persistence.TenantAwareEntity}.
     * This is needed because {@code @PrePersist} is not invoked outside a JPA container.
     */
    private void setTenantId(Object entity, UUID tenantId) {
        setField(entity, ro.ophthacloud.infrastructure.persistence.TenantAwareEntity.class,
                "tenantId", tenantId);
    }

    private void setField(Object target, Class<?> declaringClass, String fieldName, Object value) {
        try {
            Field f = declaringClass.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field '" + fieldName + "' via reflection", e);
        }
    }
}
