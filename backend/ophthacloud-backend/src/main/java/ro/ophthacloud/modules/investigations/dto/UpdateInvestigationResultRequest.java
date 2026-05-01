package ro.ophthacloud.modules.investigations.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.ophthacloud.modules.investigations.internal.InvestigationStatusType;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvestigationResultRequest {
    @NotNull(message = "status is required")
    private InvestigationStatusType status;
    
    private Instant performedAt;
    
    private JsonNode resultData;
    
    private String interpretation;
}
