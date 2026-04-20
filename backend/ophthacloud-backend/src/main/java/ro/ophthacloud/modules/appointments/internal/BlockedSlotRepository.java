package ro.ophthacloud.modules.appointments.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface BlockedSlotRepository extends JpaRepository<BlockedSlotEntity, UUID> {

    /**
     * Counts blocked slots for a doctor that overlap the proposed time window.
     * Used as part of the double-booking check (GUIDE_06 §2.2).
     */
    @Query("""
            SELECT COUNT(b) FROM BlockedSlotEntity b
            WHERE b.doctorId = :doctorId
              AND b.startAt  < :endAt
              AND b.endAt    > :startAt
            """)
    long countOverlapping(
            @Param("doctorId") UUID doctorId,
            @Param("startAt")  Instant startAt,
            @Param("endAt")    Instant endAt
    );
}
