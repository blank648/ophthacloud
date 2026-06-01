package ro.ophthacloud.modules.optical.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.ophthacloud.modules.optical.OrderStage;

import java.util.List;
import java.util.UUID;

@Repository
public interface OpticalOrderRepository extends JpaRepository<OpticalOrderEntity, UUID> {
    
    List<OpticalOrderEntity> findByTenantId(UUID tenantId);
    List<OpticalOrderEntity> findByTenantIdAndStage(UUID tenantId, OrderStage stage);

    @Query("SELECT COUNT(o) FROM OpticalOrderEntity o WHERE o.tenantId = :tenantId AND EXTRACT(YEAR FROM o.createdAt) = :year")
    long countByTenantIdAndYear(@Param("tenantId") UUID tenantId, @Param("year") int year);
}
