package fu.dlms.notification.scheduler;

import fu.dlms.notification.entity.Notification;
import fu.dlms.notification.entity.NotificationStatus;
import fu.dlms.notification.repository.NotificationRepository;
import fu.dlms.notification.sender.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Gửi MỘT thông báo và cập nhật trạng thái của nó (Task N5).
 *
 * <p>Tách khỏi NotificationDispatcher để mỗi thông báo có transaction riêng: thông báo này lỗi không làm hỏng
 * (rollback) kết quả của các thông báo khác trong cùng lượt quét. Vì Spring tạo transaction qua proxy, hàm
 * {@code @Transactional} phải được gọi từ một bean KHÁC (Dispatcher gọi sang đây) thì mới có hiệu lực.
 *
 * <p>Quy tắc thử lại: mỗi lần gửi tăng {@code attempts}. Lỗi thì lưu lý do vào {@code lastError} và dời
 * {@code scheduledAt} ra sau (lần 1 chờ 1 x retryDelay, lần 2 chờ 2 x retryDelay...). Đủ
 * {@link Notification#MAX_ATTEMPTS} lần mà vẫn lỗi thì chuyển sang FAILED; thủ thư có thể bấm gửi lại qua
 * POST /api/notifications/{id}/retry.
 */
@Slf4j
@Component
public class NotificationDelivery {

    private static final int MAX_ERROR_LENGTH = 500;

    private final NotificationRepository notificationRepository;
    private final NotificationSender sender;
    private final Clock clock;
    private final Duration retryDelay;

    public NotificationDelivery(NotificationRepository notificationRepository, NotificationSender sender, Clock clock,
                                @Value("${app.notification.retry-delay-ms:60000}") long retryDelayMs) {
        this.notificationRepository = notificationRepository;
        this.sender = sender;
        this.clock = clock;
        this.retryDelay = Duration.ofMillis(retryDelayMs);
    }

    /**
     * @return trạng thái sau khi xử lý, hoặc null nếu bỏ qua (không còn PENDING, vd đã được lượt khác gửi)
     */
    @Transactional
    public NotificationStatus deliver(Long id) {
        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null || notification.getStatus() != NotificationStatus.PENDING) {
            return null;
        }

        Instant now = clock.instant();
        notification.setAttempts(notification.getAttempts() + 1);
        try {
            sender.send(notification);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(now);
            notification.setLastError(null);
        } catch (RuntimeException e) {
            notification.setLastError(shorten(e.getClass().getSimpleName() + ": " + e.getMessage()));
            if (notification.getAttempts() >= Notification.MAX_ATTEMPTS) {
                notification.setStatus(NotificationStatus.FAILED);
                log.warn("Notification {} FAILED after {} attempts: {}", id, notification.getAttempts(), e.getMessage());
            } else {
                Instant nextTry = now.plus(retryDelay.multipliedBy(notification.getAttempts()));
                notification.setScheduledAt(nextTry);
                log.warn("Notification {} attempt {} failed, retry at {}: {}",
                        id, notification.getAttempts(), nextTry, e.getMessage());
            }
        }
        // Không cần gọi save: entity đang được JPA quản lý, thay đổi tự được ghi khi transaction commit
        return notification.getStatus();
    }

    private static String shorten(String text) {
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH);
    }
}
