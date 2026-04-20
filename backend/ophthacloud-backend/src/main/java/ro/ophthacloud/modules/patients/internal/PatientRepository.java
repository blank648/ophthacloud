package ro.ophthacloud.modules.patients.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, UUID> {

    Optional<PatientEntity> findByMrn(String mrn);

    @Query("""
            SELECT p FROM PatientEntity p
            WHERE p.isActive = true
              AND (
                LOWER(p.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.lastName)  LIKE LOWER(CONCAT('%', :q, '%')) OR
                p.phone            LIKE CONCAT('%', :q, '%')        OR
                LOWER(p.email)     LIKE LOWER(CONCAT('%', :q, '%')) OR
                p.mrn              LIKE CONCAT('%', :q, '%')
              )
            ORDER BY p.lastName ASC
            """)
    Page<PatientEntity> search(@Param("q") String query, Pageable pageable);

    @Query(value = """
            UPDATE tenants
            SET mrn_sequence = mrn_sequence + 1
            WHERE id = :tenantId
            RETURNING mrn_sequence
            """, nativeQuery = true)
    int incrementAndGetMrnSequence(@Param("tenantId") UUID tenantId);
}
