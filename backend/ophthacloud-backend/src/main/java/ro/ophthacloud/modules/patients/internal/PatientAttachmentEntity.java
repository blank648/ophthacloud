package ro.ophthacloud.modules.patients.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.util.UUID;

@Entity
@Table(name = "patient_attachments")
@Getter
@Setter
@NoArgsConstructor
public class PatientAttachmentEntity extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "storage_path", nullable = false, length = 1024)
    private String storagePath;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "uploaded_by_id")
    private UUID uploadedById;

    @Column(name = "description")
    private String description;
}
