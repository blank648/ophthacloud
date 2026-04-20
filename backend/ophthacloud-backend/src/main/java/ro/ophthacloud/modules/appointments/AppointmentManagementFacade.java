package ro.ophthacloud.modules.appointments;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.appointments.dto.*;
import ro.ophthacloud.modules.appointments.event.AppointmentBookedEvent;
import ro.ophthacloud.modules.appointments.event.AppointmentCompletedEvent;
import ro.ophthacloud.modules.appointments.event.AppointmentStatusChangedEvent;
import ro.ophthacloud.modules.appointments.event.PatientCheckedInEvent;
import ro.ophthacloud.modules.appointments.internal.*;
import ro.ophthacloud.modules.patients.PatientManagementFacade;
import ro.ophthacloud.modules.patients.dto.PatientDto;
import ro.ophthacloud.shared.audit.AuditEntry;
import ro.ophthacloud.shared.audit.AuditLogService;
import ro.ophthacloud.shared.enums.AppointmentChannel;
import ro.ophthacloud.shared.enums.AppointmentStatus;
import ro.ophthacloud.shared.security.SecurityUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * The ONLY public API of the appointments module.
 * <p>
 * Handles:
 * <ul>
 *   <li>Calendar feed queries (date-range, optional filters)</li>
 *   <li>Appointment CRUD with anti-double-booking validation (GUIDE_06 §2.2)</li>
 *   <li>Status state machine transitions with timestamp tracking (GUIDE_06 §2.3)</li>
 *   <li>Appointment type management</li>
 *   <li>All audit logging and event publishing</li>
 * </ul>
 * <p>
 * Transaction strategy: default readOnly = true; writes override individually.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AppointmentManagementFacade {

    private final AppointmentRepository       appointmentRepository;
    private final AppointmentTypeRepository   appointmentTypeRepository;
    private final BlockedSlotRepository       blockedSlotRepository;
    private final PatientManagementFacade     patientManagementFacade;
    private final AuditLogService             auditLogService;
    private final ApplicationEventPublisher   eventPublisher;

    // ── Calendar feed ────────────────────────────────────────────────────────

    /**
     * Returns a non-paginated calendar list for the given date range (max 31 days).
     * Enriches each appointment with patient name/MRN from the patients module.
     * GUIDE_04 §3.1.
     */
    public List<AppointmentDto> listCalendar(Instant from, Instant to,
                                             UUID doctorId, UUID patientId) {
        log.debug("listCalendar: tenantId={}, from={}, to={}, doctorId={}, patientId={}",
                SecurityUtils.currentTenantId(), from, to, doctorId, patientId);
        
        java.util.Map<UUID, AppointmentTypeEntity> typeMap = appointmentTypeRepository.findAllByIsActiveTrue()
                .stream().collect(java.util.stream.Collectors.toMap(AppointmentTypeEntity::getId, t -> t));

        return appointmentRepository.findCalendarRange(from, to, doctorId, patientId)
                .stream()
                .map(a -> enrichWithPatient(a, typeMap.get(a.getAppointmentTypeId())))
                .toList();
    }

    // ── Single appointment ───────────────────────────────────────────────────

    public AppointmentDto getAppointment(UUID id) {
        AppointmentEntity entity = requireAppointment(id);
        AppointmentTypeEntity type = null;
        if (entity.getAppointmentTypeId() != null) {
            type = appointmentTypeRepository.findById(entity.getAppointmentTypeId()).orElse(null);
        }
        return enrichWithPatient(entity, type);
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public AppointmentDto createAppointment(AppointmentRequest request) {
        // Validate patient exists before any DB writes (OC-014 criterion).
        if (!patientManagementFacade.patientExists(request.getPatientId())) {
            throw new IllegalArgumentException(
                    "Patient not found: " + request.getPatientId());
        }

        Instant startAt = request.getStartAt();
        Instant endAt   = startAt.plus(request.getDurationMinutes(), ChronoUnit.MINUTES);

        checkDoubleBooking(request.getDoctorId(), startAt, endAt, null);

        AppointmentEntity entity = new AppointmentEntity();
        applyRequest(entity, request, startAt, endAt);

        AppointmentEntity saved = appointmentRepository.save(entity);
        log.info("createAppointment: id={}, patient={}, doctor={}, startAt={}",
                saved.getId(), saved.getPatientId(), saved.getDoctorId(), saved.getStartAt());

        auditLogService.log(AuditEntry.builder()
                .action("CREATE")
                .entityType("Appointment")
                .entityId(saved.getId())
                .build());

        // Specific event for downstream consumers (OC-014).
        eventPublisher.publishEvent(new AppointmentBookedEvent(
                saved.getId(),
                saved.getPatientId(),
                saved.getTenantId(),
                saved.getStartAt(),
                saved.getDurationMinutes()
        ));

        AppointmentTypeEntity type = null;
        if (saved.getAppointmentTypeId() != null) {
            type = appointmentTypeRepository.findById(saved.getAppointmentTypeId()).orElse(null);
        }

        return enrichWithPatient(saved, type);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public AppointmentDto updateAppointment(UUID id, AppointmentRequest request) {
        AppointmentEntity entity = requireAppointment(id);

        AppointmentStatus status = entity.getStatus();
        if (status != AppointmentStatus.BOOKED && status != AppointmentStatus.CONFIRMED) {
            // 409 — appointment is past the modifiable window (OC-014 criterion).
            throw new AppointmentNotModifiableException(status);
        }

        Instant startAt = request.getStartAt();
        Instant endAt   = startAt.plus(request.getDurationMinutes(), ChronoUnit.MINUTES);

        checkDoubleBooking(request.getDoctorId(), startAt, endAt, id);

        applyRequest(entity, request, startAt, endAt);
        AppointmentEntity saved = appointmentRepository.save(entity);
        log.info("updateAppointment: id={}", id);

        auditLogService.log(AuditEntry.builder()
                .action("UPDATE")
                .entityType("Appointment")
                .entityId(id)
                .build());

        AppointmentTypeEntity type = null;
        if (saved.getAppointmentTypeId() != null) {
            type = appointmentTypeRepository.findById(saved.getAppointmentTypeId()).orElse(null);
        }

        return enrichWithPatient(saved, type);
    }

    // ── Status transition ────────────────────────────────────────────────────

    /**
     * Applies a status transition following the state machine defined in GUIDE_06 §2.3.
     * Records the appropriate timestamp (confirmedAt, checkedInAt, etc.) on the entity.
     * Publishes {@link AppointmentStatusChangedEvent} after commit.
     */
    @Transactional
    public AppointmentDto updateStatus(UUID id, UpdateStatusRequest request) {
        AppointmentEntity entity = requireAppointment(id);
        AppointmentStatus previous = entity.getStatus();
        AppointmentStatus next     = request.getStatus();

        if (!previous.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(previous, next);
        }

        applyStatusTransition(entity, next, request.getCancellationReason());
        AppointmentEntity saved = appointmentRepository.save(entity);

        log.info("updateStatus: id={}, {} → {}", id, previous, next);
        auditLogService.log(AuditEntry.builder()
                .action("STATUS_CHANGE")
                .entityType("Appointment")
                .entityId(id)
                .changedField("status", previous.name(), next.name())
                .build());

        eventPublisher.publishEvent(new AppointmentStatusChangedEvent(
                saved.getId(),
                saved.getPatientId(),
                saved.getTenantId(),
                previous,
                next,
                Instant.now()
        ));

        AppointmentTypeEntity type = null;
        if (saved.getAppointmentTypeId() != null) {
            type = appointmentTypeRepository.findById(saved.getAppointmentTypeId()).orElse(null);
        }

        return enrichWithPatient(saved, type);
    }

    // ── Delete (cancel) ──────────────────────────────────────────────────────

    /**
     * Soft-deletes an appointment by setting status = CANCELLED.
     * Only allowed if status is BOOKED or CONFIRMED per GUIDE_04 §3.6.
     */
    @Transactional
    public void deleteAppointment(UUID id) {
        AppointmentEntity entity = requireAppointment(id);
        AppointmentStatus status = entity.getStatus();

        if (status != AppointmentStatus.BOOKED && status != AppointmentStatus.CONFIRMED) {
            // 409 — appointment is past the modifiable window, consistent with updateAppointment.
            throw new AppointmentNotModifiableException(status);
        }

        applyStatusTransition(entity, AppointmentStatus.CANCELLED, "Deleted by user");
        appointmentRepository.save(entity);
        log.info("deleteAppointment: id={} set to CANCELLED", id);

        auditLogService.log(AuditEntry.builder()
                .action("DELETE")
                .entityType("Appointment")
                .entityId(id)
                .build());
    }

    // ── Appointment Types ─────────────────────────────────────────────────────

    public List<AppointmentTypeDto> listTypes() {
        return appointmentTypeRepository.findAllByIsActiveTrue()
                .stream()
                .map(AppointmentTypeDto::from)
                .toList();
    }

    @Transactional
    public AppointmentTypeDto createType(AppointmentTypeRequest request) {
        AppointmentTypeEntity entity = new AppointmentTypeEntity();
        applyTypeRequest(entity, request);
        AppointmentTypeEntity saved = appointmentTypeRepository.save(entity);
        log.info("createType: id={}, name={}", saved.getId(), saved.getName());
        return AppointmentTypeDto.from(saved);
    }

    @Transactional
    public AppointmentTypeDto updateType(UUID id, AppointmentTypeRequest request) {
        AppointmentTypeEntity entity = appointmentTypeRepository.findById(id)
                .filter(AppointmentTypeEntity::isActive)
                .orElseThrow(() -> new AppointmentTypeNotFoundException(id));
        applyTypeRequest(entity, request);
        AppointmentTypeEntity saved = appointmentTypeRepository.save(entity);
        log.info("updateType: id={}", id);
        return AppointmentTypeDto.from(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Double-booking check: verifies no overlapping active appointments or blocked slots
     * exist for the given doctor in the proposed [startAt, endAt] window.
     * Per GUIDE_06 §2.2.
     */
    private void checkDoubleBooking(UUID doctorId, Instant startAt, Instant endAt, UUID excludeId) {
        long overlapping = appointmentRepository.countOverlapping(doctorId, startAt, endAt, excludeId);
        if (overlapping > 0) {
            throw new DoubleBookingException(
                    "Doctor " + doctorId + " already has " + overlapping +
                    " appointment(s) overlapping " + startAt + " – " + endAt);
        }
        long blockedOverlapping = blockedSlotRepository.countOverlapping(doctorId, startAt, endAt);
        if (blockedOverlapping > 0) {
            throw new DoubleBookingException(
                    "Doctor " + doctorId + " has a blocked slot overlapping " + startAt + " – " + endAt);
        }
    }

    /**
     * Applies status transition, sets the relevant timestamp on the entity, and
     * publishes specific events for CHECKED_IN and COMPLETED per OC-014.
     * The caller is responsible for publishing the generic {@link AppointmentStatusChangedEvent}.
     */
    private void applyStatusTransition(AppointmentEntity e, AppointmentStatus next, String reason) {
        Instant now = Instant.now();
        e.setStatus(next);
        switch (next) {
            case CONFIRMED   -> e.setConfirmedAt(now);
            case CHECKED_IN  -> {
                e.setCheckedInAt(now);
                // Specific event — lets notifications know patient has arrived (OC-014).
                eventPublisher.publishEvent(new PatientCheckedInEvent(
                        e.getId(), e.getPatientId(), e.getTenantId(), now));
            }
            case COMPLETED   -> {
                e.setCompletedAt(now);
                // Specific event — consumed by reports / notifications (OC-014).
                eventPublisher.publishEvent(new AppointmentCompletedEvent(
                        e.getId(), e.getPatientId(), e.getTenantId(), now));
            }
            case NO_SHOW     -> e.setNoShowAt(now);
            case CANCELLED   -> {
                e.setCancelledAt(now);
                if (reason != null) e.setCancellationReason(reason);
            }
            default -> { /* IN_PROGRESS has no extra timestamp */ }
        }
    }

    private void applyRequest(AppointmentEntity e, AppointmentRequest r, Instant startAt, Instant endAt) {
        e.setPatientId(r.getPatientId());
        e.setDoctorId(r.getDoctorId());
        e.setDoctorName(r.getDoctorName());
        e.setAppointmentTypeId(r.getAppointmentTypeId());
        e.setStartAt(startAt);
        e.setEndAt(endAt);
        e.setDurationMinutes(r.getDurationMinutes());
        if (r.getChannel() != null) e.setChannel(r.getChannel());
        e.setRoom(r.getRoom());
        e.setChiefComplaint(r.getChiefComplaint());
        e.setPatientNotes(r.getPatientNotes());
        e.setInternalNotes(r.getInternalNotes());
    }

    private void applyTypeRequest(AppointmentTypeEntity e, AppointmentTypeRequest r) {
        e.setName(r.getName());
        if (r.getColorHex() != null) e.setColorHex(r.getColorHex());
        if (r.getDurationMinutes() != null) e.setDurationMinutes(r.getDurationMinutes());
        e.setDescription(r.getDescription());
    }

    private AppointmentEntity requireAppointment(UUID id) {
        return appointmentRepository.findById(id)
                .filter(e -> e.getStatus() != AppointmentStatus.CANCELLED)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    /**
     * Enriches an appointment entity with patient display data fetched from PatientManagementFacade,
     * and appointment type display data. Falls back gracefully.
     */
    private AppointmentDto enrichWithPatient(AppointmentEntity a, AppointmentTypeEntity type) {
        String name = null;
        String mrn  = null;
        String avatar = null;
        List<String> activeDiagnosisFlags = List.of();
        try {
            PatientDto patient = patientManagementFacade.getPatient(a.getPatientId());
            name   = patient.firstName() + " " + patient.lastName();
            mrn    = patient.mrn();
            avatar = patient.avatarUrl();
            if (patient.medicalHistory() != null && patient.medicalHistory().activeDiagnoses() != null) {
                activeDiagnosisFlags = patient.medicalHistory().activeDiagnoses().stream()
                        .map(ro.ophthacloud.modules.patients.dto.ActiveDiagnosis::icd10Code)
                        .toList();
            }
        } catch (Exception ex) {
            log.warn("Could not enrich appointment {} with patient data: {}", a.getId(), ex.getMessage());
        }
        
        String typeName = type != null ? type.getName() : null;
        String typeColor = type != null ? type.getColorHex() : null;
        
        return AppointmentDto.from(a, name, mrn, avatar, activeDiagnosisFlags, typeName, typeColor);
    }
}
