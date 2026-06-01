package ro.ophthacloud.modules.investigations.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.ophthacloud.modules.investigations.InvestigationCategoryType;
import ro.ophthacloud.modules.investigations.InvestigationStatusType;

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
    private Map<String, Object> resultData;
    private String interpretation;
    private Boolean isUrgent;
    private String notes;
    private UUID performedById;
    private Instant createdAt;
    private Instant updatedAt;
    
    private List<InvestigationFileDto> files;
}
