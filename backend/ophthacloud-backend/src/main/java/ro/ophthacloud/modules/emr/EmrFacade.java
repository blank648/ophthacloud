package ro.ophthacloud.modules.emr;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ro.ophthacloud.modules.emr.dto.*;
import ro.ophthacloud.modules.emr.internal.ConsultationService;

import java.util.List;
import java.util.UUID;

/**
 * The ONLY public API of the EMR (emr) module.
 * <p>
 * All other classes within this module are in {@code internal/} and must never be injected
 * directly from other modules — they communicate exclusively through this facade.
 * <p>
 * Delegates all work to {@link ConsultationService}.
 * Spring Modulith boundaries are enforced via {@code package-info.java} — not Java visibility.
 * <p>
 * Per GUIDE_04 §4 and GUIDE_06 §3; OC-018.
 */
@Service
@RequiredArgsConstructor
public class EmrFacade {

    private final ConsultationService consultationService;

    // ── Consultation lifecycle ────────────────────────────────────────────────

    /**
     * Creates a consultation + 9 empty section rows (A–I) in one transaction.
     * Optionally links to an appointment.
     */
    public ConsultationDto createConsultation(CreateConsultationRequest request) {
        return consultationService.createConsultation(request);
    }

    /** Returns the full consultation including all section data. */
    public ConsultationDto getConsultation(UUID id) {
        return consultationService.getConsultation(id);
    }

    /** Returns a single section by code. */
    public ConsultationSectionDto getSection(UUID consultationId, String sectionCode) {
        return consultationService.getSection(consultationId, sectionCode);
    }

    /**
     * Auto-saves section data. Auto-computes SEQ for Section A.
     * Returns 409 if consultation is SIGNED.
     */
    public ConsultationSectionDto saveSection(UUID consultationId, String sectionCode,
                                              SaveSectionRequest request) {
        return consultationService.saveSection(consultationId, sectionCode, request);
    }

    /**
     * Sets section is_completed = true and ORs the bitmask.
     * Returns updated consultation with new sectionsCompleted value.
     */
    public ConsultationDto markSectionComplete(UUID consultationId, String sectionCode) {
        return consultationService.markSectionComplete(consultationId, sectionCode);
    }

    /**
     * Signs a consultation after validating pre-conditions (GUIDE_06 §3.6).
     * Publishes ConsultationSignedEvent.
     */
    public ConsultationDto signConsultation(UUID consultationId, SignConsultationRequest request) {
        return consultationService.signConsultation(consultationId, request);
    }

    /** Returns paginated list of consultation summaries, optionally filtered by patient. */
    public Page<ConsultationSummaryDto> listConsultations(UUID patientId, Pageable pageable) {
        return consultationService.listConsultations(patientId, pageable);
    }

    // ── Clinical Templates ────────────────────────────────────────────────────

    /** Lists templates for the current tenant, optionally filtered by sectionCode. */
    public List<ClinicalTemplateDto> listTemplates(String sectionCode) {
        return consultationService.listTemplates(sectionCode);
    }

    /** Creates a new clinical template. */
    public ClinicalTemplateDto createTemplate(CreateTemplateRequest request) {
        return consultationService.createTemplate(request);
    }

    /** Updates an existing template (full replacement). */
    public ClinicalTemplateDto updateTemplate(UUID id, CreateTemplateRequest request) {
        return consultationService.updateTemplate(id, request);
    }

    /** Soft-deletes a template. */
    public void deleteTemplate(UUID id) {
        consultationService.deleteTemplate(id);
    }

    /**
     * Applies a template to a section using merge semantics — no overwrite of non-null fields.
     * Per GUIDE_06 §3.8.
     */
    public ConsultationSectionDto applyTemplate(UUID consultationId, String sectionCode, UUID templateId) {
        return consultationService.applyTemplate(consultationId, sectionCode, templateId);
    }
}
