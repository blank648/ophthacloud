package ro.unibuc.fmi.ophthacloud.patientbooking.repository;

import java.util.Optional;
import java.util.UUID;
import ro.unibuc.fmi.ophthacloud.patientbooking.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface PatientRepository extends JpaRepository<PatientEntity, UUID> {
    Optional<PatientEntity> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PatientEntity> findLockedByUserId(UUID userId);
}
