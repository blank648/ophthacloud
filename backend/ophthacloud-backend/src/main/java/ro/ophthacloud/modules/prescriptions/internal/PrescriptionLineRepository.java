package ro.ophthacloud.modules.prescriptions.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionLineRepository extends JpaRepository<PrescriptionLineEntity, UUID> {
    List<PrescriptionLineEntity> findByTenantIdAndPrescriptionId(UUID tenantId, UUID prescriptionId);
}
