package ro.ophthacloud.modules.admin.dto;

import ro.ophthacloud.modules.admin.internal.EquipmentEntity;

import java.time.Instant;
import java.util.UUID;

public record EquipmentDto(
        UUID id,
        String name,
        String brand,
        String type,
        String location,
        boolean dicomEnabled,
        Instant lastSyncAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static EquipmentDto from(EquipmentEntity entity) {
        return new EquipmentDto(
                entity.getId(),
                entity.getName(),
                entity.getBrand(),
                entity.getType(),
                entity.getLocation(),
                entity.getDicomEnabled(),
                entity.getLastSyncAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
