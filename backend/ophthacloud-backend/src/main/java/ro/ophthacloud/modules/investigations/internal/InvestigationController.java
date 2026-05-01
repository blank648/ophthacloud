package ro.ophthacloud.modules.investigations.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.ophthacloud.modules.investigations.InvestigationsFacade;
import ro.ophthacloud.modules.investigations.dto.CreateInvestigationRequest;
import ro.ophthacloud.modules.investigations.dto.InvestigationDto;
import ro.ophthacloud.modules.investigations.dto.InvestigationFileDto;
import ro.ophthacloud.modules.investigations.dto.UpdateInvestigationResultRequest;
import ro.ophthacloud.shared.api.ApiResponse;
import ro.ophthacloud.shared.api.PagedApiResponse;
import ro.ophthacloud.shared.security.SecurityUtils;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investigations")
@RequiredArgsConstructor
public class InvestigationController {

    private final InvestigationsFacade facade;

    @GetMapping
    @PreAuthorize("hasPermission('investigations', 'MODULE', 'VIEW') or hasPermission('investigations', 'MODULE', 'EDIT')")
    public PagedApiResponse<InvestigationDto> listInvestigations(
            @RequestParam UUID patientId,
            @RequestParam(required = false) InvestigationCategoryType category,
            @RequestParam(required = false) InvestigationStatusType status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderedAt"));
        Page<InvestigationDto> investigations = facade.listInvestigations(tenantId, patientId, category, status, pageable);
        
        return PagedApiResponse.of(investigations);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('investigations', 'MODULE', 'CREATE')")
    public ApiResponse<InvestigationDto> createInvestigation(@Valid @RequestBody CreateInvestigationRequest request) {
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        ro.ophthacloud.shared.security.OphthaPrincipal principal = SecurityUtils.currentPrincipal();
        UUID userId = UUID.fromString(principal.staffId() != null ? principal.staffId() : principal.keycloakUserId());
        String userName = "Dr. " + principal.staffRole(); // Or some other name derivation
        
        InvestigationDto created = facade.createInvestigation(tenantId, userId, userName, request);
        return ApiResponse.of(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('investigations', 'MODULE', 'VIEW') or hasPermission('investigations', 'MODULE', 'EDIT')")
    public ApiResponse<InvestigationDto> getInvestigation(@PathVariable UUID id) {
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        InvestigationDto investigation = facade.getInvestigation(tenantId, id);
        return ApiResponse.of(investigation);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('investigations', 'MODULE', 'EDIT')")
    public ApiResponse<InvestigationDto> updateInvestigationResult(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvestigationResultRequest request) {
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        InvestigationDto updated = facade.updateInvestigationResult(tenantId, id, request);
        return ApiResponse.of(updated);
    }

    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('investigations', 'MODULE', 'EDIT')")
    public ApiResponse<InvestigationFileDto> uploadFile(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "laterality", required = false) String laterality) {
        
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        InvestigationFileDto uploaded = facade.uploadFile(tenantId, id, file, fileType, laterality);
        return ApiResponse.of(uploaded);
    }

    @GetMapping("/{id}/files/{fileId}/download-url")
    @PreAuthorize("hasPermission('investigations', 'MODULE', 'VIEW') or hasPermission('investigations', 'MODULE', 'EDIT')")
    public ApiResponse<Map<String, String>> getFileDownloadUrl(
            @PathVariable UUID id,
            @PathVariable UUID fileId) {
        
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        String url = facade.getFileDownloadUrl(tenantId, id, fileId);
        return ApiResponse.of(Map.of("downloadUrl", url));
    }
}
