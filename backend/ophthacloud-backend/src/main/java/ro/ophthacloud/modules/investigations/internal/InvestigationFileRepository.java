package ro.ophthacloud.modules.investigations.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvestigationFileRepository extends JpaRepository<InvestigationFileEntity, UUID> {
    List<InvestigationFileEntity> findByTenantIdAndInvestigationId(UUID tenantId, UUID investigationId);
    
    long countByTenantIdAndInvestigationId(UUID tenantId, UUID investigationId);
}
