package ro.ophthacloud.modules.notifications.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, UUID> {
    Page<NotificationLogEntity> findByTenantIdAndStatus(UUID tenantId, NotificationStatus status, Pageable pageable);
    Page<NotificationLogEntity> findByTenantIdAndPatientId(UUID tenantId, UUID patientId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            SELECT n FROM NotificationLogEntity n
            WHERE n.status = 'PENDING'
               OR (n.status = 'FAILED' AND n.nextRetryAt <= CURRENT_TIMESTAMP)
            ORDER BY n.createdAt ASC
            """)
    java.util.List<NotificationLogEntity> findDueNotifications();
}
