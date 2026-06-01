package ro.ophthacloud.modules.admin.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.time.Instant;

@Entity
@Table(name = "equipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentEntity extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "brand", length = 64)
    private String brand;

    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @Column(name = "location", length = 128)
    private String location;

    @Builder.Default
    @Column(name = "dicom_enabled", nullable = false)
    private Boolean dicomEnabled = false;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;
}
