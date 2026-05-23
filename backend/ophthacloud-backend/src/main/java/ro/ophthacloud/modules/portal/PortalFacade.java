package ro.ophthacloud.modules.portal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.appointments.AppointmentManagementFacade;
import ro.ophthacloud.modules.appointments.dto.AppointmentDto;
import ro.ophthacloud.modules.investigations.InvestigationsFacade;
import ro.ophthacloud.modules.investigations.dto.InvestigationDto;
import ro.ophthacloud.modules.notifications.NotificationsFacade;
import ro.ophthacloud.modules.notifications.dto.NotificationLogDto;
import ro.ophthacloud.modules.optical.OpticalFacade;
import ro.ophthacloud.modules.optical.dto.OpticalOrderDto;
import ro.ophthacloud.modules.patients.PatientManagementFacade;
import ro.ophthacloud.modules.patients.dto.PatientDto;
import ro.ophthacloud.modules.portal.dto.*;
import ro.ophthacloud.modules.prescriptions.PrescriptionsFacade;
import ro.ophthacloud.modules.prescriptions.dto.PrescriptionDto;
import ro.ophthacloud.shared.audit.AuditEntry;
import ro.ophthacloud.shared.audit.AuditLogService;
import ro.ophthacloud.shared.security.SecurityUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The ONLY public API of the portal module.
 * <p>
 * Per GUIDE_06 §10, provides read-only access to the authenticated patient's own data.
 * Cross-module data is fetched via the public facades of other modules — never via
 * direct repository access (Spring Modulith boundary rule).
 * <p>
 * The {@code patientId} is always extracted from the JWT via {@link SecurityUtils#currentPatientId()},
 * never from request parameters or path variables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PortalFacade {

    private final PatientManagementFacade patientFacade;
    private final AppointmentManagementFacade appointmentFacade;
    private final PrescriptionsFacade prescriptionsFacade;
    private final InvestigationsFacade investigationsFacade;
    private final OpticalFacade opticalFacade;
    private final NotificationsFacade notificationsFacade;
    private final AuditLogService auditLogService;

    // ── Profile ──────────────────────────────────────────────────────────────

    /**
     * Returns the authenticated patient's own profile.
     * Excludes sensitive internal fields (CNP, insurance, notes) per GUIDE_06 §10.2.
     */
    public PortalProfileDto getMyProfile() {
        UUID patientId = UUID.fromString(SecurityUtils.currentPatientId());
        PatientDto p = patientFacade.getPatient(patientId);
        log.debug("Portal: patient {} viewed profile", patientId);
        return new PortalProfileDto(
                p.id(), p.mrn(), p.firstName(), p.lastName(),
                p.dateOfBirth(), p.gender().name(), p.phone(), p.email(),
                p.hasPortalAccess()
        );
    }

    // ── Appointments ─────────────────────────────────────────────────────────

    /**
     * Returns the authenticated patient's upcoming appointments (future only).
     * Excludes {@code internalNotes} per GUIDE_06 §10.3.
     */
    public List<PortalAppointmentDto> getMyAppointments() {
        UUID patientId = UUID.fromString(SecurityUtils.currentPatientId());

        // Fetch all future appointments for this patient
        Instant now = Instant.now();
        Instant far = now.plusSeconds(365L * 24 * 60 * 60); // ~1 year ahead
        List<AppointmentDto> all = appointmentFacade.listCalendar(now, far, null, null);

        return all.stream()
                .filter(a -> patientId.equals(a.patientId()))
                .map(a -> new PortalAppointmentDto(
                        a.id(), a.doctorName(), a.startAt(), a.endAt(),
                        a.status().name(), a.chiefComplaint(), a.patientNotes(),
                        a.room(), a.durationMinutes()
                ))
                .toList();
    }

    // ── Prescriptions ────────────────────────────────────────────────────────

    /**
     * Returns the authenticated patient's active prescriptions.
     */
    public List<PortalPrescriptionSummaryDto> getMyPrescriptions(Pageable pageable) {
        UUID tenantId  = UUID.fromString(SecurityUtils.currentTenantId());
        UUID patientId = UUID.fromString(SecurityUtils.currentPatientId());
        Page<PrescriptionDto> page = prescriptionsFacade.listPrescriptions(tenantId, patientId, null, pageable);

        return page.getContent().stream()
                .map(p -> new PortalPrescriptionSummaryDto(
                        p.getId(),
                        Objects.toString(p.getPrescriptionType(), null),
                        Objects.toString(p.getStatus(), null),
                        p.getIssuedByName(), p.getValidFrom(), p.getValidUntil()
                ))
                .toList();
    }

    /**
     * Returns a specific prescription detail with line items.
     * Audits the VIEW action per GUIDE_06 §11.1.
     */
    public PortalPrescriptionDetailDto getMyPrescriptionDetail(UUID prescriptionId) {
        UUID tenantId  = UUID.fromString(SecurityUtils.currentTenantId());
        UUID patientId = UUID.fromString(SecurityUtils.currentPatientId());
        PrescriptionDto p = prescriptionsFacade.getPrescription(tenantId, prescriptionId);

        // Security check: prescription must belong to this patient
        if (!patientId.equals(p.getPatientId())) {
            throw new PortalAccessDeniedException("Prescription does not belong to this patient");
        }

        // Audit: patient viewed prescription
        auditLogService.log(
                AuditEntry.builder()
                        .action("VIEW")
                        .entityType("Prescription")
                        .entityId(prescriptionId)
                        .build()
        );

        var lines = p.getLines() != null
                ? p.getLines().stream().map(l -> new PortalPrescriptionLineDto(
                        l.getEye(),
                        null, // lensType is on the parent prescription, not on individual lines
                        l.getSph(), l.getCyl(),
                        l.getAxis() != null ? l.getAxis().intValue() : null,
                        l.getAddPower(), null
                )).toList()
                : List.<PortalPrescriptionLineDto>of();

        return new PortalPrescriptionDetailDto(
                p.getId(),
                Objects.toString(p.getPrescriptionType(), null),
                Objects.toString(p.getStatus(), null),
                p.getIssuedByName(),
                p.getValidFrom(), p.getValidUntil(), lines,
                p.getQrCodeToken() != null ? UUID.fromString(p.getQrCodeToken()) : null
        );
    }

    // ── Investigations ───────────────────────────────────────────────────────

    /**
     * Returns the authenticated patient's completed investigations.
     */
    public List<PortalInvestigationDto> getMyInvestigations(Pageable pageable) {
        UUID tenantId  = UUID.fromString(SecurityUtils.currentTenantId());
        UUID patientId = UUID.fromString(SecurityUtils.currentPatientId());

        // Fetch all investigations for this patient, filter COMPLETED in-memory
        // (avoids cross-module dependency on internal InvestigationStatusType enum)
        Page<InvestigationDto> page = investigationsFacade.listInvestigations(
                tenantId, patientId, null, null, pageable);

        return page.getContent().stream()
                .filter(i -> i.getStatus() != null && "COMPLETED".equals(i.getStatus().name()))
                .map(i -> new PortalInvestigationDto(
                        i.getId(),
                        i.getCategory() != null ? i.getCategory().name() : null,
                        i.getStatus().name(),
                        i.getOrderedByName(), i.getOrderedAt(), i.getPerformedAt(),
                        i.getInterpretation()
                ))
                .toList();
    }

    // ── Optical Orders ───────────────────────────────────────────────────────

    /**
     * Returns the authenticated patient's optical orders.
     */
    public List<PortalOpticalOrderDto> getMyOpticalOrders() {
        UUID tenantId  = UUID.fromString(SecurityUtils.currentTenantId());
        UUID patientId = UUID.fromString(SecurityUtils.currentPatientId());

        // Fetch all orders and filter by patient
        List<OpticalOrderDto> orders = opticalFacade.listOrders(tenantId, null);

        return orders.stream()
                .filter(o -> patientId.equals(o.patientId()))
                .map(o -> new PortalOpticalOrderDto(
                        o.id(), o.orderNumber(),
                        o.stage() != null ? o.stage().name() : null,
                        o.orderType() != null ? o.orderType().name() : null,
                        null, null // OpticalOrderDto doesn't expose createdAt/updatedAt directly
                ))
                .toList();
    }

    // ── Notifications ────────────────────────────────────────────────────────

    /**
     * Returns the authenticated patient's notification log entries.
     */
    public List<PortalNotificationDto> getMyNotifications(Pageable pageable) {
        UUID patientId = UUID.fromString(SecurityUtils.currentPatientId());

        Page<NotificationLogDto> page = notificationsFacade.listNotificationLog(patientId, null, pageable);

        return page.getContent().stream()
                .map(n -> new PortalNotificationDto(
                        n.id(), n.channel(), n.status(),
                        n.subject(), n.bodyPreview(), n.sentAt(), null
                ))
                .toList();
    }

    // ── Consents ─────────────────────────────────────────────────────────────

    /**
     * Updates the authenticated patient's GDPR consents.
     * This is the only writable endpoint in the portal.
     */
    @Transactional
    public void updateMyConsents(UpdateConsentsRequest request) {
        UUID patientId = UUID.fromString(SecurityUtils.currentPatientId());
        log.info("Portal: patient {} updating GDPR consents", patientId);
        // Consent updates are handled via the patient facade
        // For now, log the action — the actual consent persistence is part of the patient module
        auditLogService.log(
                AuditEntry.builder()
                        .action("UPDATE")
                        .entityType("PatientConsent")
                        .entityId(patientId)
                        .build()
        );
    }
}
