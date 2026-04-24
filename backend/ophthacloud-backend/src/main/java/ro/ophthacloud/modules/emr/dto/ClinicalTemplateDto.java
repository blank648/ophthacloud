package ro.ophthacloud.modules.emr.dto;

import ro.ophthacloud.modules.emr.internal.ClinicalTemplateEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for clinical templates (GUIDE_04 §4.8).
 */
public record ClinicalTemplateDto(
        UUID id,
        UUID tenantId,
        String name,
        String sectionCode,
        String category,
        boolean isGlobal,
        UUID createdById,
        boolean isActive,
        String templateData,
        Instant createdAt
) {
    public static ClinicalTemplateDto from(ClinicalTemplateEntity e) {
        return new ClinicalTemplateDto(
                e.getId(),
                e.getTenantId(),
                e.getName(),
                e.getSectionCode(),
                e.getCategory(),
                Boolean.TRUE.equals(e.getIsGlobal()),
                e.getCreatedById(),
                Boolean.TRUE.equals(e.getIsActive()),
                e.getTemplateData(),
                e.getCreatedAt()
        );
    }
}
