package ro.ophthacloud.modules.optical.internal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockItemRepository extends JpaRepository<StockItemEntity, UUID> {
    
    List<StockItemEntity> findByTenantIdAndCurrentStockLessThanEqual(UUID tenantId, int threshold);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockItemEntity s WHERE s.id = :id")
    Optional<StockItemEntity> findByIdForUpdate(@Param("id") UUID id);
}
