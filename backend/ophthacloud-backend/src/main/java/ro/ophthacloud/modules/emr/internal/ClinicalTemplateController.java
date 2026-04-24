package ro.ophthacloud.modules.emr.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.emr.EmrFacade;
import ro.ophthacloud.modules.emr.dto.ClinicalTemplateDto;
import ro.ophthacloud.modules.emr.dto.CreateTemplateRequest;
import ro.ophthacloud.shared.api.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for clinical templates.
 * Internal to the emr module.
 * <p>
 * Per GUIDE_04 §4 and GUIDE_05 §5.2; OC-019.
 */
@RestController
@RequestMapping("/api/v1/emr/templates")
@RequiredArgsConstructor
@Tag(name = "Clinical Templates", description = "Endpoints for clinical template management")
public class ClinicalTemplateController {

    private final EmrFacade emrFacade;

    @Operation(summary = "List clinical templates", description = "Returns a list of templates, optionally filtered by section code.")
    @GetMapping
    @PreAuthorize("hasPermission('emr', 'MODULE', 'VIEW')")
    public ApiResponse<List<ClinicalTemplateDto>> listTemplates(@RequestParam(required = false) String sectionCode) {
        return ApiResponse.of(emrFacade.listTemplates(sectionCode));
    }

    @Operation(summary = "Create clinical template", description = "Creates a new clinical template for a specific section.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('emr', 'MODULE', 'EDIT')")
    public ApiResponse<ClinicalTemplateDto> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        return ApiResponse.of(emrFacade.createTemplate(request));
    }

    @Operation(summary = "Update clinical template", description = "Updates an existing clinical template.")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('emr', 'MODULE', 'EDIT')")
    public ApiResponse<ClinicalTemplateDto> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTemplateRequest request) {
        return ApiResponse.of(emrFacade.updateTemplate(id, request));
    }

    @Operation(summary = "Delete clinical template", description = "Soft deletes a clinical template.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasPermission('emr', 'MODULE', 'EDIT')")
    public void deleteTemplate(@PathVariable UUID id) {
        emrFacade.deleteTemplate(id);
    }
}
