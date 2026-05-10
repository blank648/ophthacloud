package ro.ophthacloud.modules.notifications.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
public class NotificationLogEntity extends TenantAwareEntity {

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "recipient_address", nullable = false)
    private String recipientAddress;

    @Column(name = "subject")
    private String subject;

    @Column(name = "body_preview")
    private String bodyPreview;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "external_message_id")
    private String externalMessageId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
}
