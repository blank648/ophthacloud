package ro.ophthacloud.modules.optical.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalogEntity, UUID> {
    
    List<ServiceCatalogEntity> findByTenantId(UUID tenantId);
    
    List<ServiceCatalogEntity> findByTenantIdAndIsActiveTrue(UUID tenantId);
}
