package ro.ophthacloud.modules.appointments;

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
import org.springframework.security.core.context.SecurityContextHolder;
import ro.ophthacloud.infrastructure.persistence.BaseEntity;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;
import ro.ophthacloud.modules.appointments.dto.AppointmentRequest;
import ro.ophthacloud.modules.appointments.dto.UpdateStatusRequest;
import ro.ophthacloud.modules.appointments.event.AppointmentBookedEvent;
import ro.ophthacloud.modules.appointments.event.AppointmentCompletedEvent;
import ro.ophthacloud.modules.appointments.event.PatientCheckedInEvent;
import ro.ophthacloud.modules.appointments.internal.AppointmentEntity;
import ro.ophthacloud.modules.appointments.internal.AppointmentRepository;
import ro.ophthacloud.modules.appointments.internal.AppointmentTypeRepository;
import ro.ophthacloud.modules.appointments.internal.BlockedSlotRepository;
import ro.ophthacloud.modules.patients.PatientManagementFacade;
import ro.ophthacloud.modules.patients.dto.PatientDto;
import ro.ophthacloud.shared.audit.AuditEntry;
import ro.ophthacloud.shared.audit.AuditLogService;
import ro.ophthacloud.shared.enums.AppointmentStatus;
import ro.ophthacloud.shared.security.ModulePermissions;
import ro.ophthacloud.shared.security.OphthaClinicalAuthenticationToken;
import ro.ophthacloud.shared.security.OphthaPrincipal;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AppointmentManagementFacade}.
 * No Spring context — pure Mockito. All acceptance criteria for OC-016 unit tier:
 * create-success, endAt-computation, double-booking-reject (appointment + blocked slot),
 * patient-not-found guard, all valid state-machine transitions (×4 paths),
 * invalid transition, update-blocked-by-status (409).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AppointmentManagementFacade")
class AppointmentManagementFacadeTest {

    @Mock private AppointmentRepository       appointmentRepository;
    @Mock private AppointmentTypeRepository   appointmentTypeRepository;
    @Mock private BlockedSlotRepository       blockedSlotRepository;
    @Mock private PatientManagementFacade     patientManagementFacade;
    @Mock private AuditLogService             auditLogService;
    @Mock private ApplicationEventPublisher   eventPublisher;

    @InjectMocks
    private AppointmentManagementFacade facade;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID  = UUID.randomUUID();
    private static final UUID APPT_ID    = UUID.randomUUID();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @BeforeEach
    void setUpContext() {
        TenantContext.set(TENANT_ID);
        OphthaPrincipal principal = new OphthaPrincipal(
                UUID.randomUUID().toString(),
                TENANT_ID.toString(),
                UUID.randomUUID().toString(),
                "DOCTOR",
                Map.of("appointments", new ModulePermissions(true, true, true, true, false, false))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new OphthaClinicalAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ── Test 1: create-success ────────────────────────────────────────────────

    @Test
    @DisplayName("createAppointment: should save and return AppointmentDto when request is valid")
    void createAppointment_shouldSaveAndReturnDto_whenRequestIsValid() {
        when(patientManagementFacade.patientExists(PATIENT_ID)).thenReturn(true);
        when(appointmentRepository.countOverlapping(any(), any(), any(), any())).thenReturn(0L);
        when(blockedSlotRepository.countOverlapping(any(), any(), any())).thenReturn(0L);
        when(appointmentRepository.save(any())).thenAnswer(inv -> {
            AppointmentEntity e = inv.getArgument(0);
            setId(e, APPT_ID);
            setTenantId(e, TENANT_ID);
            return e;
        });
        when(appointmentTypeRepository.findById(any())).thenReturn(Optional.empty());
        when(patientManagementFacade.getPatient(PATIENT_ID)).thenReturn(stubPatientDto());
        doNothing().when(auditLogService).log(any(AuditEntry.class));

        var result = facade.createAppointment(baseRequest());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(APPT_ID);
        assertThat(result.patientId()).isEqualTo(PATIENT_ID);
    }

    // ── Test 2: endAt computation ─────────────────────────────────────────────

    @Test
    @DisplayName("createAppointment: should compute endAt as startAt + durationMinutes at runtime")
    void createAppointment_shouldComputeEndAt_asStartPlusDuration() {
        AppointmentRequest req = baseRequest();
        Instant expectedEndAt = req.getStartAt().plus(req.getDurationMinutes(), ChronoUnit.MINUTES);

        when(patientManagementFacade.patientExists(PATIENT_ID)).thenReturn(true);
        when(appointmentRepository.countOverlapping(any(), any(), any(), any())).thenReturn(0L);
        when(blockedSlotRepository.countOverlapping(any(), any(), any())).thenReturn(0L);
        when(appointmentRepository.save(any())).thenAnswer(inv -> {
            AppointmentEntity e = inv.getArgument(0);
            setId(e, UUID.randomUUID());
            setTenantId(e, TENANT_ID);
            return e;
        });
        when(appointmentTypeRepository.findById(any())).thenReturn(Optional.empty());
        when(patientManagementFacade.getPatient(PATIENT_ID)).thenReturn(stubPatientDto());
        doNothing().when(auditLogService).log(any(AuditEntry.class));

        var result = facade.createAppointment(req);

        // endAt is never stored — it is recalculated in AppointmentDto.from()
        assertThat(result.endAt()).isEqualTo(expectedEndAt);
    }

    // ── Test 3: double-booking — overlapping appointment ─────────────────────

    @Test
    @DisplayName("createAppointment: should throw DoubleBookingException when appointment overlaps")
    void createAppointment_shouldThrowDoubleBooking_whenAppointmentOverlaps() {
        when(patientManagementFacade.patientExists(PATIENT_ID)).thenReturn(true);
        when(appointmentRepository.countOverlapping(
                eq(DOCTOR_ID), any(Instant.class), any(Instant.class), eq(null)))
                .thenReturn(1L);

        assertThatThrownBy(() -> facade.createAppointment(baseRequest()))
                .isInstanceOf(DoubleBookingException.class);
    }

    // ── Test 4: double-booking — blocked slot ────────────────────────────────

    @Test
    @DisplayName("createAppointment: should throw DoubleBookingException when blocked slot overlaps")
    void createAppointment_shouldThrowDoubleBooking_whenBlockedSlotOverlaps() {
        when(patientManagementFacade.patientExists(PATIENT_ID)).thenReturn(true);
        when(appointmentRepository.countOverlapping(any(), any(), any(), any())).thenReturn(0L);
        when(blockedSlotRepository.countOverlapping(
                eq(DOCTOR_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(1L);

        assertThatThrownBy(() -> facade.createAppointment(baseRequest()))
                .isInstanceOf(DoubleBookingException.class);
    }

    // ── Test 5: patient not found guard ──────────────────────────────────────

    @Test
    @DisplayName("createAppointment: should throw IllegalArgumentException when patient does not exist")
    void createAppointment_shouldThrow_whenPatientDoesNotExist() {
        when(patientManagementFacade.patientExists(PATIENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> facade.createAppointment(baseRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PATIENT_ID.toString());
    }

    // ── Test 6: valid transition BOOKED → CONFIRMED ───────────────────────────

    @Test
    @DisplayName("updateStatus: should transition from BOOKED to CONFIRMED and record confirmedAt")
    void updateStatus_shouldTransition_fromBookedToConfirmed() {
        AppointmentEntity entity = appointmentEntityWithStatus(APPT_ID, AppointmentStatus.BOOKED);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(entity));
        when(appointmentRepository.save(any())).thenReturn(entity);
        when(appointmentTypeRepository.findById(any())).thenReturn(Optional.empty());
        when(patientManagementFacade.getPatient(PATIENT_ID)).thenReturn(stubPatientDto());
        doNothing().when(auditLogService).log(any(AuditEntry.class));

        var result = facade.updateStatus(APPT_ID, new UpdateStatusRequest(AppointmentStatus.CONFIRMED, null));

        assertThat(result.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(entity.getConfirmedAt()).isNotNull();
    }

    // ── Test 7: valid transition CONFIRMED → CHECKED_IN ──────────────────────

    @Test
    @DisplayName("updateStatus: should transition from CONFIRMED to CHECKED_IN and publish PatientCheckedInEvent")
    void updateStatus_shouldTransition_fromConfirmedToCheckedIn() {
        AppointmentEntity entity = appointmentEntityWithStatus(APPT_ID, AppointmentStatus.CONFIRMED);
        entity.setConfirmedAt(Instant.now().minusSeconds(60));
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(entity));
        when(appointmentRepository.save(any())).thenReturn(entity);
        when(appointmentTypeRepository.findById(any())).thenReturn(Optional.empty());
        when(patientManagementFacade.getPatient(PATIENT_ID)).thenReturn(stubPatientDto());
        doNothing().when(auditLogService).log(any(AuditEntry.class));

        facade.updateStatus(APPT_ID, new UpdateStatusRequest(AppointmentStatus.CHECKED_IN, null));

        assertThat(entity.getCheckedInAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(PatientCheckedInEvent.class));
    }

    // ── Test 8: valid transition CHECKED_IN → IN_PROGRESS ────────────────────

    @Test
    @DisplayName("updateStatus: should transition from CHECKED_IN to IN_PROGRESS")
    void updateStatus_shouldTransition_fromCheckedInToInProgress() {
        AppointmentEntity entity = appointmentEntityWithStatus(APPT_ID, AppointmentStatus.CHECKED_IN);
        entity.setCheckedInAt(Instant.now().minusSeconds(60));
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(entity));
        when(appointmentRepository.save(any())).thenReturn(entity);
        when(appointmentTypeRepository.findById(any())).thenReturn(Optional.empty());
        when(patientManagementFacade.getPatient(PATIENT_ID)).thenReturn(stubPatientDto());
        doNothing().when(auditLogService).log(any(AuditEntry.class));

        var result = facade.updateStatus(APPT_ID, new UpdateStatusRequest(AppointmentStatus.IN_PROGRESS, null));

        assertThat(result.status()).isEqualTo(AppointmentStatus.IN_PROGRESS);
    }

    // ── Test 9: valid transition IN_PROGRESS → COMPLETED ─────────────────────

    @Test
    @DisplayName("updateStatus: should transition from IN_PROGRESS to COMPLETED and publish AppointmentCompletedEvent")
    void updateStatus_shouldTransition_fromInProgressToCompleted() {
        AppointmentEntity entity = appointmentEntityWithStatus(APPT_ID, AppointmentStatus.IN_PROGRESS);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(entity));
        when(appointmentRepository.save(any())).thenReturn(entity);
        when(appointmentTypeRepository.findById(any())).thenReturn(Optional.empty());
        when(patientManagementFacade.getPatient(PATIENT_ID)).thenReturn(stubPatientDto());
        doNothing().when(auditLogService).log(any(AuditEntry.class));

        facade.updateStatus(APPT_ID, new UpdateStatusRequest(AppointmentStatus.COMPLETED, null));

        assertThat(entity.getCompletedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(AppointmentCompletedEvent.class));
    }

    // ── Test 10: invalid transition → InvalidStatusTransitionException ─────────

    @Test
    @DisplayName("updateStatus: should throw InvalidStatusTransitionException when COMPLETED→BOOKED attempted")
    void updateStatus_shouldThrowInvalidTransition_whenCompletedToBooked() {
        AppointmentEntity entity = appointmentEntityWithStatus(APPT_ID, AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> facade.updateStatus(
                APPT_ID, new UpdateStatusRequest(AppointmentStatus.BOOKED, null)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ── Test 11: updateAppointment blocked when status is CHECKED_IN → 409 ───

    @Test
    @DisplayName("updateAppointment: should throw AppointmentNotModifiableException when status is CHECKED_IN")
    void updateAppointment_shouldThrowNotModifiable_whenStatusIsCheckedIn() {
        AppointmentEntity entity = appointmentEntityWithStatus(APPT_ID, AppointmentStatus.CHECKED_IN);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> facade.updateAppointment(APPT_ID, baseRequest()))
                .isInstanceOf(AppointmentNotModifiableException.class);
    }

    // ── Test 12: AppointmentBookedEvent published on create ───────────────────

    @Test
    @DisplayName("createAppointment: should publish AppointmentBookedEvent after successful save")
    void createAppointment_shouldPublishAppointmentBookedEvent() {
        when(patientManagementFacade.patientExists(PATIENT_ID)).thenReturn(true);
        when(appointmentRepository.countOverlapping(any(), any(), any(), any())).thenReturn(0L);
        when(blockedSlotRepository.countOverlapping(any(), any(), any())).thenReturn(0L);
        when(appointmentRepository.save(any())).thenAnswer(inv -> {
            AppointmentEntity e = inv.getArgument(0);
            setId(e, APPT_ID);
            setTenantId(e, TENANT_ID);
            return e;
        });
        when(appointmentTypeRepository.findById(any())).thenReturn(Optional.empty());
        when(patientManagementFacade.getPatient(PATIENT_ID)).thenReturn(stubPatientDto());
        doNothing().when(auditLogService).log(any(AuditEntry.class));

        facade.createAppointment(baseRequest());

        ArgumentCaptor<AppointmentBookedEvent> captor = ArgumentCaptor.forClass(AppointmentBookedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        AppointmentBookedEvent event = captor.getValue();
        assertThat(event.appointmentId()).isEqualTo(APPT_ID);
        assertThat(event.patientId()).isEqualTo(PATIENT_ID);
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AppointmentRequest baseRequest() {
        return AppointmentRequest.builder()
                .patientId(PATIENT_ID)
                .doctorId(DOCTOR_ID)
                .doctorName("Dr. Test")
                .startAt(Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS))
                .durationMinutes(30)
                .build();
    }

    private AppointmentEntity appointmentEntityWithStatus(UUID id, AppointmentStatus status) {
        AppointmentEntity e = new AppointmentEntity();
        setId(e, id);
        setTenantId(e, TENANT_ID);
        e.setPatientId(PATIENT_ID);
        e.setDoctorId(DOCTOR_ID);
        e.setDoctorName("Dr. Test");
        e.setStartAt(Instant.now().plus(1, ChronoUnit.DAYS));
        e.setEndAt(Instant.now().plus(1, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES));
        e.setDurationMinutes(30);
        e.setStatus(status);
        return e;
    }

    private PatientDto stubPatientDto() {
        return new PatientDto(
                PATIENT_ID,       // id
                "OC-000001",      // mrn
                "Ion",            // firstName
                "Popescu",        // lastName
                LocalDate.of(1985, 6, 15), // dateOfBirth
                40,               // age
                null,             // gender
                null,             // cnp
                "0700000000",     // phone
                null,             // phoneAlt
                null,             // email
                null,             // address
                null,             // city
                null,             // county
                null,             // bloodType
                null,             // occupation
                null,             // employer
                null,             // emergencyContactName
                null,             // emergencyContactPhone
                null,             // insuranceProvider
                null,             // insuranceNumber
                null,             // referringDoctor
                false,            // hasPortalAccess
                null,             // portalInvitedAt
                true,             // isActive
                null,             // notes
                null,             // avatarUrl
                null,             // medicalHistory
                null,             // createdAt
                null              // updatedAt
        );
    }

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
