package ro.ophthacloud.modules.investigations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestigationFileDto {
    private UUID id;
    private String fileName;
    private String mimeType;
    private Long fileSizeBytes;
    private String fileType;
    private String laterality;
    private String downloadUrl;
    private Instant createdAt;
}
