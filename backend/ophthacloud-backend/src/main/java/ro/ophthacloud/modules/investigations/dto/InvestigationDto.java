package ro.ophthacloud.modules.investigations.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.ophthacloud.modules.investigations.internal.InvestigationCategoryType;
import ro.ophthacloud.modules.investigations.internal.InvestigationStatusType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestigationDto {
    private UUID id;
    private UUID patientId;
    private UUID consultationId;
    private UUID orderedById;
    private String orderedByName;
    private InvestigationCategoryType category;
    private String name;
    private String device;
    private InvestigationStatusType status;
    private Instant orderedAt;
    private Instant performedAt;
    private JsonNode resultData;
    private String interpretation;
    private Boolean isUrgent;
    private String notes;
    private UUID performedById;
    private Instant createdAt;
    private Instant updatedAt;
    
    private List<InvestigationFileDto> files;
}
