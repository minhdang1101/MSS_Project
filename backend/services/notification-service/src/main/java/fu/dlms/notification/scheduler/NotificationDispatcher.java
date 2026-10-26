package fu.dlms.notification.scheduler;

import fu.dlms.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Sends PENDING notifications whose scheduled_at has passed. */
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;

    @Scheduled(fixedDelayString = "${app.notification.dispatch-interval-ms:60000}")
    public void dispatch() {
        // TODO: send via channel (email/SMS), set SENT + sentAt or FAILED
    }
}
