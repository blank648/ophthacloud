package ro.ophthacloud.modules.prescriptions.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<PrescriptionEntity, UUID> {

    Optional<PrescriptionEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<PrescriptionEntity> findByTenantIdAndPatientId(UUID tenantId, UUID patientId, Pageable pageable);

    Page<PrescriptionEntity> findByTenantIdAndPatientIdAndStatus(
            UUID tenantId, UUID patientId, PrescriptionStatusType status, Pageable pageable);

    /**
     * Used to check for prescription number collisions within a tenant+year.
     */
    @Query("SELECT COUNT(p) FROM PrescriptionEntity p WHERE p.tenantId = :tenantId AND p.prescriptionNumber LIKE :prefix%")
    long countByTenantIdAndPrescriptionNumberStartingWith(
            @Param("tenantId") UUID tenantId, @Param("prefix") String prefix);

    /**
     * Supersede all ACTIVE prescriptions of the same type for a patient.
     * Used when a new prescription of the same type is signed.
     */
    @Modifying
    @Query("""
            UPDATE PrescriptionEntity p
            SET p.status = ro.ophthacloud.modules.prescriptions.internal.PrescriptionStatusType.SUPERSEDED,
                p.supersededById = :newId
            WHERE p.patientId = :patientId
              AND p.tenantId = :tenantId
              AND p.prescriptionType = :type
              AND p.status = ro.ophthacloud.modules.prescriptions.internal.PrescriptionStatusType.ACTIVE
              AND p.id != :newId
            """)
    int supersedePreviousActive(
            @Param("patientId") UUID patientId,
            @Param("tenantId") UUID tenantId,
            @Param("type") PrescriptionType type,
            @Param("newId") UUID newId);

    /**
     * Used by QR public verification endpoint — no tenant filter needed.
     */
    Optional<PrescriptionEntity> findByQrCodeToken(UUID qrCodeToken);

    /**
     * Finds all ACTIVE prescriptions past valid_until — for the nightly expiry scheduler.
     */
    @Query(value = """
            SELECT * FROM prescriptions
            WHERE status = 'ACTIVE'
              AND valid_until < CURRENT_DATE
            """, nativeQuery = true)
    List<PrescriptionEntity> findExpired();
}
