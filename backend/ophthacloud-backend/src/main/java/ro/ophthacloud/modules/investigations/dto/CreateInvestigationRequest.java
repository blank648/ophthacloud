package ro.ophthacloud.modules.investigations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.ophthacloud.modules.investigations.InvestigationCategoryType;

import java.util.UUID;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvestigationRequest {
    @NotNull(message = "patientId is required")
    private UUID patientId;
    
    private UUID consultationId;
    
    @NotNull(message = "category is required")
    private InvestigationCategoryType category;
    
    @NotBlank(message = "name is required")
    private String name;
    
    private String device;
    private Boolean isUrgent;
    private String notes;
}
