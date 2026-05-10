package ro.ophthacloud.modules.notifications.internal.dispatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import ro.ophthacloud.modules.notifications.internal.NotificationChannel;
import ro.ophthacloud.modules.notifications.internal.NotificationLogEntity;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailDispatcher implements NotificationDispatcher {

    private final JavaMailSender mailSender;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public void dispatch(NotificationLogEntity notification) throws Exception {
        log.debug("Dispatching email to {}", notification.getRecipientAddress());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(notification.getRecipientAddress());
        if (notification.getSubject() != null) {
            message.setSubject(notification.getSubject());
        } else {
            message.setSubject("OphthaCloud Notification");
        }
        message.setText(notification.getBodyPreview());
        mailSender.send(message);
    }
}
