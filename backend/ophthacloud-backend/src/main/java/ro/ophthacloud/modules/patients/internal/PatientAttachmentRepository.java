package ro.ophthacloud.modules.patients.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientAttachmentRepository extends JpaRepository<PatientAttachmentEntity, UUID> {
    List<PatientAttachmentEntity> findByPatientId(UUID patientId);
}
