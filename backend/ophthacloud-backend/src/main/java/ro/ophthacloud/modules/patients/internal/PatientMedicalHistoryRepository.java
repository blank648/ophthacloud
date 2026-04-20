package ro.ophthacloud.modules.patients.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientMedicalHistoryRepository extends JpaRepository<PatientMedicalHistoryEntity, UUID> {
    Optional<PatientMedicalHistoryEntity> findByPatientId(UUID patientId);
}
