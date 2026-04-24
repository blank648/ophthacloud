package ro.ophthacloud.modules.emr.dto;

import ro.ophthacloud.modules.emr.internal.ConsultationSectionEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a single consultation section.
 * sectionData is returned as a raw JSON string (JSONB passthrough).
 * Per GUIDE_04 §4.3 and §4.4.
 */
public record ConsultationSectionDto(
        UUID id,
        UUID consultationId,
        String sectionCode,
        boolean isCompleted,
        Instant completedAt,
        String sectionData,
        Instant updatedAt
) {
    public static ConsultationSectionDto from(ConsultationSectionEntity e) {
        return new ConsultationSectionDto(
                e.getId(),
                e.getConsultationId(),
                e.getSectionCode(),
                e.isCompleted(),
                e.getCompletedAt(),
                e.getSectionData(),
                e.getUpdatedAt()
        );
    }
}
