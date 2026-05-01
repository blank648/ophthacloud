package ro.ophthacloud.modules.investigations.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestigationRepository extends JpaRepository<InvestigationEntity, UUID> {
    Optional<InvestigationEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<InvestigationEntity> findByTenantIdAndPatientId(UUID tenantId, UUID patientId, Pageable pageable);

    Page<InvestigationEntity> findByTenantIdAndPatientIdAndCategory(UUID tenantId, UUID patientId, InvestigationCategoryType category, Pageable pageable);

    Page<InvestigationEntity> findByTenantIdAndPatientIdAndStatus(UUID tenantId, UUID patientId, InvestigationStatusType status, Pageable pageable);

    Page<InvestigationEntity> findByTenantIdAndPatientIdAndCategoryAndStatus(UUID tenantId, UUID patientId, InvestigationCategoryType category, InvestigationStatusType status, Pageable pageable);
}
