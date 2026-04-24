package ro.ophthacloud.modules.emr.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.emr.EmrFacade;
import ro.ophthacloud.modules.emr.dto.*;
import ro.ophthacloud.shared.api.ApiResponse;

import java.util.UUID;

/**
 * REST controller for EMR consultation management.
 * Internal to the emr module.
 * <p>
 * Per GUIDE_04 §4 and GUIDE_05 §5.2; OC-019.
 */
@RestController
@RequestMapping("/api/v1/emr/consultations")
@RequiredArgsConstructor
@Tag(name = "EMR Consultations", description = "Endpoints for electronic medical record consultation management")
public class ConsultationController {

    private final EmrFacade emrFacade;

    @Operation(summary = "Create a new consultation", description = "Initializes a consultation record and 9 empty sections.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('emr', 'MODULE', 'EDIT')")
    public ApiResponse<ConsultationDto> createConsultation(@Valid @RequestBody CreateConsultationRequest request) {
        return ApiResponse.of(emrFacade.createConsultation(request));
    }

    @Operation(summary = "Get full consultation detail", description = "Returns the consultation and all its sections.")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('emr', 'MODULE', 'VIEW')")
    public ApiResponse<ConsultationDto> getConsultation(@PathVariable UUID id) {
        return ApiResponse.of(emrFacade.getConsultation(id));
    }

    @Operation(summary = "Get a specific consultation section", description = "Returns JSON data for a single section (A-I).")
    @GetMapping("/{id}/sections/{code}")
    @PreAuthorize("hasPermission('emr', 'MODULE', 'VIEW')")
    public ApiResponse<ConsultationSectionDto> getSection(
            @PathVariable UUID id,
            @PathVariable String code) {
        return ApiResponse.of(emrFacade.getSection(id, code));
    }

    @Operation(summary = "Save section data", description = "Updates section JSON data. Auto-computes SEQ for Section A.")
    @PutMapping("/{id}/sections/{code}")
    @PreAuthorize("hasPermission('emr', 'MODULE', 'EDIT')")
    public ApiResponse<ConsultationSectionDto> saveSection(
            @PathVariable UUID id,
            @PathVariable String code,
            @Valid @RequestBody SaveSectionRequest request) {
        return ApiResponse.of(emrFacade.saveSection(id, code, request));
    }

    @Operation(summary = "Mark section as complete", description = "Updates bitmask and section status.")
    @PatchMapping("/{id}/sections/{code}/complete")
    @PreAuthorize("hasPermission('emr', 'MODULE', 'EDIT')")
    public ApiResponse<ConsultationDto> markSectionComplete(
            @PathVariable UUID id,
            @PathVariable String code) {
        return ApiResponse.of(emrFacade.markSectionComplete(id, code));
    }

    @Operation(summary = "Digitally sign consultation", description = "Finalizes the consultation. Requires diagnostic Section G completion.")
    @PostMapping("/{id}/sign")
    @PreAuthorize("hasPermission('emr', 'MODULE', 'SIGN')")
    public ApiResponse<ConsultationDto> signConsultation(
            @PathVariable UUID id,
            @Valid @RequestBody SignConsultationRequest request) {
        return ApiResponse.of(emrFacade.signConsultation(id, request));
    }

    @Operation(summary = "List consultations", description = "Paginated list of consultations, optionally filtered by patient.")
    @GetMapping
    @PreAuthorize("hasPermission('emr', 'MODULE', 'VIEW')")
    public ApiResponse<Page<ConsultationSummaryDto>> listConsultations(
            @RequestParam(required = false) UUID patientId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.of(emrFacade.listConsultations(patientId, pageable));
    }

    @Operation(summary = "Apply template to section", description = "Merges template data into section without overwriting non-null fields.")
    @PostMapping("/{id}/sections/{code}/apply-template/{templateId}")
    @PreAuthorize("hasPermission('emr', 'MODULE', 'EDIT')")
    public ApiResponse<ConsultationSectionDto> applyTemplate(
            @PathVariable UUID id,
            @PathVariable String code,
            @PathVariable UUID templateId) {
        return ApiResponse.of(emrFacade.applyTemplate(id, code, templateId));
    }
}
