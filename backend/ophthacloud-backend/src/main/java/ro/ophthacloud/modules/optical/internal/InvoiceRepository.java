package ro.ophthacloud.modules.optical.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {
    
    List<InvoiceEntity> findByTenantIdAndStatus(UUID tenantId, InvoiceStatus status);

    @Query("SELECT COUNT(i) FROM InvoiceEntity i WHERE i.tenantId = :tenantId AND EXTRACT(YEAR FROM i.createdAt) = :year")
    long countByTenantIdAndYear(@Param("tenantId") UUID tenantId, @Param("year") int year);
}
