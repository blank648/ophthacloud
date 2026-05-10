package ro.ophthacloud.modules.notifications.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.notifications.internal.dispatcher.NotificationDispatcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Periodically polls the database for notifications that need to be sent.
 * Handles dispatching and retry logic with exponential backoff (GUIDE_06 §7.5).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationLogRepository notificationLogRepository;
    private final List<NotificationDispatcher> dispatchers;

    /**
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void processPendingNotifications() {
        log.debug("Starting NotificationScheduler poll...");

        List<NotificationLogEntity> dueNotifications = notificationLogRepository.findDueNotifications();
        if (dueNotifications.isEmpty()) {
            return;
        }

        log.info("Found {} due notifications to dispatch", dueNotifications.size());

        for (NotificationLogEntity notification : dueNotifications) {
            dispatch(notification);
        }
    }

    private void dispatch(NotificationLogEntity notification) {
        NotificationDispatcher dispatcher = dispatchers.stream()
                .filter(d -> d.supports(notification.getChannel()))
                .findFirst()
                .orElse(null);

        if (dispatcher == null) {
            log.error("No dispatcher found for channel: {}", notification.getChannel());
            markAsFailed(notification, "No dispatcher available for channel");
            return;
        }

        try {
            dispatcher.dispatch(notification);
            markAsSent(notification);
        } catch (Exception ex) {
            log.warn("Failed to dispatch notification {}: {}", notification.getId(), ex.getMessage());
            markAsFailed(notification, ex.getMessage());
        }
    }

    private void markAsSent(NotificationLogEntity notification) {
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(Instant.now());
        notification.setFailureReason(null);
        notificationLogRepository.save(notification);
    }

    private void markAsFailed(NotificationLogEntity notification, String reason) {
        int retries = notification.getRetryCount() + 1;
        notification.setRetryCount(retries);
        notification.setFailedAt(Instant.now());
        notification.setFailureReason(reason);

        // GUIDE_06 §7.5: 3 attempts with exponential backoff (immediately, 15min, 1h)
        // Attempt 1 fails -> next in 15m (retries = 1)
        // Attempt 2 fails -> next in 1h (retries = 2)
        // Attempt 3 fails -> FAILED_FINAL (retries = 3)
        if (retries >= 3) {
            notification.setStatus(NotificationStatus.FAILED_FINAL);
            notification.setNextRetryAt(null);
            log.error("Notification {} reached max retries. Status -> FAILED_FINAL", notification.getId());
            // In a full implementation, send admin alert here
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            if (retries == 1) {
                notification.setNextRetryAt(Instant.now().plus(15, ChronoUnit.MINUTES));
            } else if (retries == 2) {
                notification.setNextRetryAt(Instant.now().plus(1, ChronoUnit.HOURS));
            }
        }
        notificationLogRepository.save(notification);
    }
}
