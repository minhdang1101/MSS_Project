package fu.dlms.notification.dto.res;

import fu.dlms.notification.entity.Notification;
import fu.dlms.notification.entity.NotificationChannel;
import fu.dlms.notification.entity.NotificationStatus;
import fu.dlms.notification.entity.NotificationType;

import java.time.Instant;

/**
 * Dữ liệu trả cho client. Cố ý không có idempotencyKey và sourceEventId: đó là chi tiết nội bộ để chống trùng.
 */
public record NotificationResponse(
        Long id,
        Long memberId,
        Long loanId,
        NotificationType type,
        NotificationChannel channel,
        String recipient,
        String subject,
        String message,
        Instant scheduledAt,
        Instant sentAt,
        NotificationStatus status,
        int attempts,
        String lastError,
        Instant readAt,
        Instant createdAt
) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getMemberId(), n.getLoanId(), n.getType(), n.getChannel(),
                n.getRecipient(), n.getSubject(), n.getMessage(), n.getScheduledAt(), n.getSentAt(), n.getStatus(),
                n.getAttempts(), n.getLastError(), n.getReadAt(), n.getCreatedAt());
    }
}
