package ro.ophthacloud.modules.emr.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsultationRepository extends JpaRepository<ConsultationEntity, UUID> {
    Page<ConsultationEntity> findByPatientId(UUID patientId, Pageable pageable);
    Page<ConsultationEntity> findByPatientIdAndTenantId(UUID patientId, UUID tenantId, Pageable pageable);
    Page<ConsultationEntity> findByTenantId(UUID tenantId, Pageable pageable);

    /** Tenant-safe primary-key lookup — use instead of findById to enforce @Filter for PK queries. */
    Optional<ConsultationEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
