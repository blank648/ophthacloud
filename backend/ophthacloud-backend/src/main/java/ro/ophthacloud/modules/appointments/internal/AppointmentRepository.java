package ro.ophthacloud.modules.appointments.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {

    /**
     * Calendar feed query: returns all appointments for a tenant overlapping the given date range.
     * Supports optional doctorId and patientId filters (null = no filter).
     * GUIDE_06 §2.2 anti-double booking uses a separate overlap query.
     */
    @Query("""
            SELECT a FROM AppointmentEntity a
            WHERE a.startAt < :to
              AND a.endAt   > :from
              AND (:doctorId  IS NULL OR a.doctorId  = :doctorId)
              AND (:patientId IS NULL OR a.patientId = :patientId)
            ORDER BY a.startAt ASC
            """)
    List<AppointmentEntity> findCalendarRange(
            @Param("from")      Instant from,
            @Param("to")        Instant to,
            @Param("doctorId")  UUID doctorId,
            @Param("patientId") UUID patientId
    );

    /**
     * Anti-double booking check (GUIDE_06 §2.2).
     * Returns count of active overlapping appointments for the given doctor,
     * excluding the appointment being updated (excludeId = null for creates).
     */
    @Query("""
            SELECT COUNT(a) FROM AppointmentEntity a
            WHERE a.doctorId = :doctorId
              AND a.status NOT IN (
                  ro.ophthacloud.shared.enums.AppointmentStatus.CANCELLED,
                  ro.ophthacloud.shared.enums.AppointmentStatus.NO_SHOW,
                  ro.ophthacloud.shared.enums.AppointmentStatus.COMPLETED
              )
              AND (:excludeId IS NULL OR a.id != :excludeId)
              AND a.startAt < :endAt
              AND a.endAt   > :startAt
            """)
    long countOverlapping(
            @Param("doctorId")  UUID doctorId,
            @Param("startAt")   Instant startAt,
            @Param("endAt")     Instant endAt,
            @Param("excludeId") UUID excludeId
    );
}
