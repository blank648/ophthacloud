package ro.ophthacloud.modules.patients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.patients.internal.PatientEntity;
import ro.ophthacloud.modules.patients.internal.PatientMedicalHistoryEntity;
import ro.ophthacloud.modules.patients.internal.PatientRepository;
import ro.ophthacloud.modules.patients.dto.CreatePatientRequest;
import ro.ophthacloud.modules.patients.dto.PatientDto;
import ro.ophthacloud.modules.patients.dto.PatientSummaryDto;
import ro.ophthacloud.modules.patients.dto.UpdatePatientRequest;
import ro.ophthacloud.modules.patients.event.PatientCreatedEvent;
import ro.ophthacloud.modules.patients.PatientNotFoundException;
import ro.ophthacloud.shared.audit.AuditEntry;
import ro.ophthacloud.shared.audit.AuditLogService;
import ro.ophthacloud.shared.security.SecurityUtils;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.time.Instant;
import java.util.UUID;

/**
 * The ONLY public API of the patients module.
 * <p>
 * All other classes within this module are package-private or in {@code internal/} and must
 * never be injected directly from other modules — they communicate exclusively through this facade.
 * <p>
 * Transaction strategy (GUIDE_07 §2.4):
 * <ul>
 *   <li>Default {@code readOnly = true} on the class — inherited by all read methods</li>
 *   <li>Individual write methods override with {@code @Transactional}</li>
 *   <li>External calls (Keycloak, MinIO) are intentionally placed OUTSIDE DB transactions</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PatientManagementFacade {

    private final PatientRepository patientRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    // ── Read operations ──────────────────────────────────────────────────────

    /**
     * Returns a paginated list of patients matching the search query.
     * If {@code q} is blank, returns all active patients ordered by last name.
     * Tenant isolation enforced by the Hibernate tenant filter on the session.
     */
    public Page<PatientSummaryDto> listPatients(String q, Pageable pageable) {
        String query = (q != null && !q.isBlank()) ? q.strip() : "";
        log.debug("listPatients: tenantId={}, q='{}', page={}", SecurityUtils.currentTenantId(), query, pageable.getPageNumber());
        return patientRepository.search(query, pageable).map(PatientSummaryDto::from);
    }

    /**
     * Returns true if an active patient with the given ID exists in the current tenant.
     * Used by other modules (e.g. appointments) for a lightweight existence check before
     * performing cross-module operations. Does not emit an audit log entry.
     */
    public boolean patientExists(UUID patientId) {
        return patientRepository.findById(patientId)
                .filter(PatientEntity::isActive)
                .isPresent();
    }

    /**
     * Returns the full patient record including medical history.
     *
     * @throws PatientNotFoundException if no active patient exists with the given ID
     */
    public PatientDto getPatient(UUID patientId) {
        log.debug("getPatient: id={}", patientId);
        PatientEntity patient = requirePatient(patientId);
        auditLogService.log(AuditEntry.builder()
                .action("VIEW")
                .entityType("Patient")
                .entityId(patientId)
                .build());
        return PatientDto.from(patient);
    }

    // ── Write operations ─────────────────────────────────────────────────────

    /**
     * Creates a new patient with an auto-generated MRN.
     * <p>
     * MRN generation uses a DB-level {@code UPDATE … RETURNING} on the tenant's
     * {@code mrn_sequence} counter, which is atomic and race-safe (GUIDE_06 §1.1).
     */
    @Transactional
    public PatientDto createPatient(CreatePatientRequest request) {
        UUID tenantId = TenantContext.require();
        log.debug("createPatient: tenantId={}, name={} {}", tenantId, request.getFirstName(), request.getLastName());

        String mrn = generateMrn(tenantId);

        PatientEntity patient = new PatientEntity();
        patient.setMrn(mrn);
        applyRequest(patient, request);

        // Create a blank medical history record linked to patient
        PatientMedicalHistoryEntity history = new PatientMedicalHistoryEntity();
        history.setPatient(patient);
        patient.setMedicalHistory(history);

        PatientEntity saved = patientRepository.save(patient);
        log.info("createPatient: mrn={}, id={}, tenantId={}", saved.getMrn(), saved.getId(), saved.getTenantId());

        auditLogService.log(AuditEntry.builder()
                .action("CREATE")
                .entityType("Patient")
                .entityId(saved.getId())
                .changedField("mrn", null, saved.getMrn())
                .build());

        eventPublisher.publishEvent(new PatientCreatedEvent(saved.getId(), saved.getMrn(), saved.getTenantId()));

        return PatientDto.from(saved);
    }

    /**
     * Fully replaces a patient's demographic data (PUT semantics).
     * MRN is never changed.
     *
     * @throws PatientNotFoundException if no active patient exists with the given ID
     */
    @Transactional
    public PatientDto updatePatient(UUID patientId, UpdatePatientRequest request) {
        PatientEntity patient = requirePatient(patientId);
        applyRequest(patient, request);

        PatientEntity saved = patientRepository.save(patient);
        log.info("updatePatient: id={}, tenantId={}", saved.getId(), saved.getTenantId());

        auditLogService.log(AuditEntry.builder()
                .action("UPDATE")
                .entityType("Patient")
                .entityId(saved.getId())
                .build());

        return PatientDto.from(saved);
    }

    /**
     * Soft-deletes a patient by setting {@code isActive = false}.
     *
     * @throws PatientNotFoundException if no active patient exists with the given ID
     */
    @Transactional
    public void deletePatient(UUID patientId) {
        PatientEntity patient = requirePatient(patientId);
        patient.setActive(false);
        patientRepository.save(patient);
        log.info("deletePatient (soft): id={}, tenantId={}", patientId, patient.getTenantId());

        auditLogService.log(AuditEntry.builder()
                .action("DELETE")
                .entityType("Patient")
                .entityId(patientId)
                .build());
    }

    /**
     * Sends a portal invite to the patient (sets hasPortalAccess = true, records timestamp).
     * Actual Keycloak user creation and email are handled OUTSIDE this transaction
     * by an event listener on PatientPortalInvitedEvent.
     *
     * @throws PatientNotFoundException if no active patient exists with the given ID
     * @throws IllegalStateException    if the patient has no email or already has portal access
     */
    @Transactional
    public Instant inviteToPortal(UUID patientId) {
        PatientEntity patient = requirePatient(patientId);

        if (patient.getEmail() == null || patient.getEmail().isBlank()) {
            throw new IllegalStateException("Patient must have an email address to receive a portal invite.");
        }
        if (patient.isHasPortalAccess()) {
            throw new IllegalStateException("Patient already has portal access.");
        }

        Instant now = Instant.now();
        patient.setHasPortalAccess(true);
        patient.setPortalInvitedAt(now);
        patientRepository.save(patient);

        auditLogService.log(AuditEntry.builder()
                .action("PORTAL_INVITE_SENT")
                .entityType("Patient")
                .entityId(patientId)
                .build());

        log.info("inviteToPortal: id={}, email={}", patientId, patient.getEmail());
        return now;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Generates the next MRN for the given tenant using an atomic DB counter increment.
     * Format: {@code OC-000001} to {@code OC-999999} (GUIDE_06 §1.1).
     */
    private String generateMrn(UUID tenantId) {
        int seq = patientRepository.incrementAndGetMrnSequence(tenantId);
        return "OC-%06d".formatted(seq);
    }

    /**
     * Loads a patient by ID, ensuring it is active.
     * Throws {@link PatientNotFoundException} if not found.
     */
    private PatientEntity requirePatient(UUID patientId) {
        return patientRepository.findById(patientId)
                .filter(PatientEntity::isActive)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
    }

    /** Applies CreatePatientRequest fields onto a patient entity. */
    private void applyRequest(PatientEntity p, CreatePatientRequest r) {
        p.setFirstName(r.getFirstName());
        p.setLastName(r.getLastName());
        p.setDateOfBirth(r.getDateOfBirth());
        p.setGender(r.getGender());
        p.setPhone(r.getPhone());
        p.setPhoneAlt(r.getPhoneAlt());
        p.setEmail(r.getEmail());
        p.setCnp(r.getCnp());
        p.setAddress(r.getAddress());
        p.setCity(r.getCity());
        p.setCounty(r.getCounty());
        p.setBloodType(r.getBloodType());
        p.setOccupation(r.getOccupation());
        p.setEmployer(r.getEmployer());
        p.setEmergencyContactName(r.getEmergencyContactName());
        p.setEmergencyContactPhone(r.getEmergencyContactPhone());
        p.setInsuranceProvider(r.getInsuranceProvider());
        p.setInsuranceNumber(r.getInsuranceNumber());
        p.setReferringDoctor(r.getReferringDoctor());
        p.setNotes(r.getNotes());
    }

    /** Applies UpdatePatientRequest fields onto a patient entity (same fields, different request type). */
    private void applyRequest(PatientEntity p, UpdatePatientRequest r) {
        p.setFirstName(r.getFirstName());
        p.setLastName(r.getLastName());
        p.setDateOfBirth(r.getDateOfBirth());
        p.setGender(r.getGender());
        p.setPhone(r.getPhone());
        p.setPhoneAlt(r.getPhoneAlt());
        p.setEmail(r.getEmail());
        p.setCnp(r.getCnp());
        p.setAddress(r.getAddress());
        p.setCity(r.getCity());
        p.setCounty(r.getCounty());
        p.setBloodType(r.getBloodType());
        p.setOccupation(r.getOccupation());
        p.setEmployer(r.getEmployer());
        p.setEmergencyContactName(r.getEmergencyContactName());
        p.setEmergencyContactPhone(r.getEmergencyContactPhone());
        p.setInsuranceProvider(r.getInsuranceProvider());
        p.setInsuranceNumber(r.getInsuranceNumber());
        p.setReferringDoctor(r.getReferringDoctor());
        p.setNotes(r.getNotes());
    }
}
