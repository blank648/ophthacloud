package ro.ophthacloud.modules.notifications.internal.dispatcher;

import ro.ophthacloud.modules.notifications.internal.NotificationChannel;
import ro.ophthacloud.modules.notifications.internal.NotificationLogEntity;

/**
 * Strategy interface for dispatching notifications across different channels.
 */
public interface NotificationDispatcher {
    boolean supports(NotificationChannel channel);
    void dispatch(NotificationLogEntity notification) throws Exception;
}
