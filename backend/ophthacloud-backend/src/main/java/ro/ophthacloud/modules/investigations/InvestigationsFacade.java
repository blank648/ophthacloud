package ro.ophthacloud.modules.investigations;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ro.ophthacloud.modules.investigations.dto.CreateInvestigationRequest;
import ro.ophthacloud.modules.investigations.dto.InvestigationDto;
import ro.ophthacloud.modules.investigations.dto.InvestigationFileDto;
import ro.ophthacloud.modules.investigations.dto.UpdateInvestigationResultRequest;
import ro.ophthacloud.modules.investigations.internal.InvestigationService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvestigationsFacade {

    private final InvestigationService investigationService;

    public InvestigationDto createInvestigation(UUID tenantId, UUID orderedById, String orderedByName, CreateInvestigationRequest request) {
        return investigationService.createInvestigation(tenantId, orderedById, orderedByName, request);
    }

    public InvestigationDto getInvestigation(UUID tenantId, UUID id) {
        return investigationService.getInvestigation(tenantId, id);
    }

    public Page<InvestigationDto> listInvestigations(UUID tenantId, UUID patientId, InvestigationCategoryType category, InvestigationStatusType status, Pageable pageable) {
        return investigationService.listInvestigations(tenantId, patientId, category, status, pageable);
    }

    public InvestigationDto updateInvestigationResult(UUID tenantId, UUID id, UpdateInvestigationResultRequest request) {
        return investigationService.updateInvestigationResult(tenantId, id, request);
    }

    public InvestigationFileDto uploadFile(UUID tenantId, UUID investigationId, MultipartFile file, String fileType, String laterality) {
        return investigationService.uploadFile(tenantId, investigationId, file, fileType, laterality);
    }

    public String getFileDownloadUrl(UUID tenantId, UUID investigationId, UUID fileId) {
        return investigationService.getFileDownloadUrl(tenantId, investigationId, fileId);
    }
}
