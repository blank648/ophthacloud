package ro.ophthacloud.modules.optical.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockItemRepository extends JpaRepository<StockItemEntity, UUID> {
    
    List<StockItemEntity> findByTenantIdAndCurrentStockLessThanEqual(UUID tenantId, int threshold);
}
