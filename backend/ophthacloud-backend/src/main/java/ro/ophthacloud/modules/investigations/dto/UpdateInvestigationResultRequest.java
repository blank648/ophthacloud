package ro.ophthacloud.modules.investigations.dto;

import java.util.Map;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.ophthacloud.modules.investigations.InvestigationStatusType;

import java.time.Instant;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvestigationResultRequest {
    @NotNull(message = "status is required")
    private InvestigationStatusType status;
    
    private Instant performedAt;
    
    private Map<String, Object> resultData;
    
    private String interpretation;
}
