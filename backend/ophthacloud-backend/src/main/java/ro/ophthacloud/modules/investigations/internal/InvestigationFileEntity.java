package ro.ophthacloud.modules.investigations.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.util.UUID;

@Entity
@Table(name = "investigation_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestigationFileEntity extends TenantAwareEntity {

    @Column(name = "investigation_id", nullable = false)
    private UUID investigationId;

    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;

    @Column(name = "storage_path", nullable = false, length = 1024)
    private String storagePath;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_type", length = 32)
    private String fileType;

    @Column(name = "laterality", length = 8)
    private String laterality;
}
