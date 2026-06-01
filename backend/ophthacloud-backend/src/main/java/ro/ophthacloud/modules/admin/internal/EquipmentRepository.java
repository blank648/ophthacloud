package ro.ophthacloud.modules.admin.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EquipmentRepository extends JpaRepository<EquipmentEntity, UUID> {
    List<EquipmentEntity> findAllByTenantIdOrderByNameAsc(UUID tenantId);
}
