package ro.ophthacloud.modules.investigations.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ro.ophthacloud.modules.investigations.dto.CreateInvestigationRequest;
import ro.ophthacloud.modules.investigations.dto.InvestigationDto;
import ro.ophthacloud.modules.investigations.dto.InvestigationFileDto;
import ro.ophthacloud.modules.investigations.dto.UpdateInvestigationResultRequest;
import ro.ophthacloud.modules.investigations.event.InvestigationResultAvailableEvent;
import ro.ophthacloud.modules.investigations.InvestigationCategoryType;
import ro.ophthacloud.modules.investigations.InvestigationStatusType;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvestigationService {

    private final InvestigationRepository investigationRepository;
    private final InvestigationFileRepository investigationFileRepository;
    private final MinioStorageService minioStorageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InvestigationDto createInvestigation(UUID tenantId, UUID orderedById, String orderedByName, CreateInvestigationRequest request) {
        InvestigationEntity entity = InvestigationEntity.builder()
                .patientId(request.getPatientId())
                .consultationId(request.getConsultationId())
                .orderedById(orderedById)
                .orderedByName(orderedByName)
                .category(request.getCategory())
                .name(request.getName())
                .device(request.getDevice())
                .status(InvestigationStatusType.ORDERED)
                .orderedAt(Instant.now())
                .isUrgent(request.getIsUrgent() != null ? request.getIsUrgent() : false)
                .notes(request.getNotes())
                .build();
        
        InvestigationEntity saved = investigationRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public InvestigationDto getInvestigation(UUID tenantId, UUID id) {
        InvestigationEntity entity = getEntityOrThrow(tenantId, id);
        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<InvestigationDto> listInvestigations(UUID tenantId, UUID patientId, InvestigationCategoryType category, InvestigationStatusType status, Pageable pageable) {
        Page<InvestigationEntity> page;
        if (category != null && status != null) {
            page = investigationRepository.findByTenantIdAndPatientIdAndCategoryAndStatus(tenantId, patientId, category, status, pageable);
        } else if (category != null) {
            page = investigationRepository.findByTenantIdAndPatientIdAndCategory(tenantId, patientId, category, pageable);
        } else if (status != null) {
            page = investigationRepository.findByTenantIdAndPatientIdAndStatus(tenantId, patientId, status, pageable);
        } else {
            page = investigationRepository.findByTenantIdAndPatientId(tenantId, patientId, pageable);
        }
        return page.map(this::mapToDto);
    }

    @Transactional
    public InvestigationDto updateInvestigationResult(UUID tenantId, UUID id, UpdateInvestigationResultRequest request) {
        InvestigationEntity entity = getEntityOrThrow(tenantId, id);
        
        entity.setStatus(request.getStatus());
        entity.setPerformedAt(request.getPerformedAt());
        String jsonResult = null;
        if (request.getResultData() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                jsonResult = mapper.writeValueAsString(request.getResultData());
            } catch (Exception e) {}
        }
        entity.setResultData(jsonResult);
        entity.setInterpretation(request.getInterpretation());
        
        InvestigationEntity updated = investigationRepository.save(entity);
        
        if (updated.getStatus() == InvestigationStatusType.COMPLETED) {
            eventPublisher.publishEvent(new InvestigationResultAvailableEvent(
                updated.getId(),
                updated.getPatientId(),
                updated.getTenantId(),
                updated.getCategory(),
                updated.getUpdatedAt()
            ));
        }
        
        return mapToDto(updated);
    }

    @Transactional
    public InvestigationFileDto uploadFile(UUID tenantId, UUID investigationId, MultipartFile file, String fileType, String laterality) {
        InvestigationEntity investigation = getEntityOrThrow(tenantId, investigationId);
        
        long fileCount = investigationFileRepository.countByTenantIdAndInvestigationId(tenantId, investigationId);
        if (fileCount >= 20) {
            throw new IllegalArgumentException("Maximum of 20 files allowed per investigation");
        }
        
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 50MB limit");
        }
        
        String originalFilename = file.getOriginalFilename();
        String uuidFileName = UUID.randomUUID().toString() + "-" + originalFilename;
        String storagePath = tenantId.toString() + "/investigations/" + investigationId.toString() + "/" + uuidFileName;
        
        try (InputStream inputStream = file.getInputStream()) {
            minioStorageService.uploadFile(storagePath, inputStream, file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file", e);
        }
        
        InvestigationFileEntity fileEntity = InvestigationFileEntity.builder()
                .investigationId(investigationId)
                .fileName(originalFilename)
                .storagePath(storagePath)
                .mimeType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .fileType(fileType)
                .laterality(laterality)
                .build();
        investigationFileRepository.save(fileEntity);
        
        if (investigation.getStatus() == InvestigationStatusType.ORDERED) {
            investigation.setStatus(InvestigationStatusType.IN_PROGRESS);
            investigationRepository.save(investigation);
        }
        
        return mapToFileDto(fileEntity);
    }

    @Transactional(readOnly = true)
    public String getFileDownloadUrl(UUID tenantId, UUID investigationId, UUID fileId) {
        InvestigationFileEntity fileEntity = investigationFileRepository.findById(fileId)
                .filter(f -> f.getTenantId().equals(tenantId) && f.getInvestigationId().equals(investigationId))
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
                
        return minioStorageService.getPresignedUrl(fileEntity.getStoragePath());
    }

    private InvestigationEntity getEntityOrThrow(UUID tenantId, UUID id) {
        return investigationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new InvestigationNotFoundException(id));
    }

    private InvestigationDto mapToDto(InvestigationEntity entity) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map<String, Object> resultDataNode = null;
        if (entity.getResultData() != null && !entity.getResultData().isEmpty()) {
            try {
                resultDataNode = mapper.readValue(entity.getResultData(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
            } catch (Exception ignored) {
            }
        }
        
        InvestigationDto dto = InvestigationDto.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .consultationId(entity.getConsultationId())
                .orderedById(entity.getOrderedById())
                .orderedByName(entity.getOrderedByName())
                .category(entity.getCategory())
                .name(entity.getName())
                .device(entity.getDevice())
                .status(entity.getStatus())
                .orderedAt(entity.getOrderedAt())
                .performedAt(entity.getPerformedAt())
                .resultData(resultDataNode)
                .interpretation(entity.getInterpretation())
                .isUrgent(entity.isUrgent())
                .notes(entity.getNotes())
                .performedById(entity.getPerformedById())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
                
        List<InvestigationFileEntity> files = investigationFileRepository.findByTenantIdAndInvestigationId(entity.getTenantId(), entity.getId());
        dto.setFiles(files.stream().map(this::mapToFileDto).collect(Collectors.toList()));
        
        return dto;
    }

    private InvestigationFileDto mapToFileDto(InvestigationFileEntity entity) {
        return InvestigationFileDto.builder()
                .id(entity.getId())
                .fileName(entity.getFileName())
                .mimeType(entity.getMimeType())
                .fileSizeBytes(entity.getFileSizeBytes())
                .fileType(entity.getFileType())
                .laterality(entity.getLaterality())
                .createdAt(entity.getCreatedAt())
                .downloadUrl(minioStorageService.getPresignedUrl(entity.getStoragePath()))
                .build();
    }
}
