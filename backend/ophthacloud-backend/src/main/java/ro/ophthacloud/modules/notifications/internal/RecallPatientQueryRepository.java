package ro.ophthacloud.modules.notifications.internal;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface RecallPatientQueryRepository extends Repository<RecallProtocolEntity, UUID> {
    
    interface RecallCandidate {
        UUID getPatientId();
        String getEmail();
        String getFirstName();
        String getLastName();
    }

    @Query(value = """
            SELECT p.id as patientId, p.email as email, p.first_name as firstName, p.last_name as lastName
            FROM patients p
            JOIN patient_medical_history pmh ON p.id = pmh.patient_id
            WHERE p.is_active = true
              AND p.tenant_id = :tenantId
              AND pmh.active_diagnoses::text LIKE CONCAT('%"icd10Code":"', :icd10Code, '"%')
              AND EXISTS (
                  SELECT 1 FROM consultations c
                  WHERE c.patient_id = p.id
                    AND c.status = 'SIGNED'
                    AND c.consultation_date <= :cutoffDate
              )
              AND NOT EXISTS (
                  SELECT 1 FROM appointments a
                  WHERE a.patient_id = p.id
                    AND a.status IN ('BOOKED', 'CONFIRMED')
                    AND a.start_at > CURRENT_TIMESTAMP
              )
              AND NOT EXISTS (
                  SELECT 1 FROM notification_log nl
                  WHERE nl.patient_id = p.id
                    AND nl.tenant_id = :tenantId
                    AND nl.rule_id = :ruleId
                    AND nl.created_at > CURRENT_TIMESTAMP - INTERVAL '30 days'
              )
            """, nativeQuery = true)
    List<RecallCandidate> findCandidates(
            @Param("tenantId") UUID tenantId,
            @Param("icd10Code") String icd10Code,
            @Param("ruleId") UUID ruleId,
            @Param("cutoffDate") LocalDate cutoffDate
    );
}
