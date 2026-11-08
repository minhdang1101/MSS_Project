package fu.dlms.notification.service.impl;

import fu.dlms.notification.dto.NotificationResponse;
import fu.dlms.notification.entity.Notification;
import fu.dlms.notification.entity.NotificationChannel;
import fu.dlms.notification.entity.NotificationStatus;
import fu.dlms.notification.entity.NotificationType;
import fu.dlms.notification.messaging.LoanEvent;
import fu.dlms.notification.repository.NotificationRepository;
import fu.dlms.notification.service.NotificationService;
import fu.dlms.notification.service.NotificationTemplates;
import fu.dlms.notification.service.ReminderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Cài đặt nghiệp vụ thông báo.
 *
 * <p>{@code @Transactional(readOnly = true)} ở mức class: mặc định mọi hàm chỉ đọc dữ liệu. Hàm nào cần ghi thì
 * khai báo lại {@code @Transactional} riêng (như {@link #createReminder}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplates templates;

    @Override
    public List<NotificationResponse> findAll(Long memberId) {
        var notifications = memberId == null
                ? notificationRepository.findAll()
                : notificationRepository.findByMemberId(memberId);
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Override
    @Transactional
    public ReminderResult createReminder(NotificationType type, LoanEvent event) {
        // Khóa chống trùng, ví dụ "DUE_SOON:15" (xem NotificationType.idempotencyKey)
        String key = type.idempotencyKey(event.loanId());

        // Lớp chống trùng 1: hỏi database trước. Chặn được gần như mọi trường hợp gửi lại
        // (RabbitMQ giao lại, outbox gửi lại, loan-service phát lại vào ngày hôm sau).
        if (notificationRepository.existsByIdempotencyKey(key)) {
            return ReminderResult.DUPLICATE;
        }

        Notification notification = new Notification();
        notification.setIdempotencyKey(key);
        notification.setSourceEventId(event.eventId()); // chỉ để truy vết sự kiện gốc
        notification.setMemberId(event.memberId());
        notification.setLoanId(event.loanId());
        notification.setType(type);
        // Tạm gửi trong ứng dụng (IN_APP), người nhận xác định bằng memberId.
        // Task N4 sẽ gọi member-service lấy email để chuyển sang kênh EMAIL.
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setRecipient("member:" + event.memberId());
        notification.setSubject(templates.subject(type));
        notification.setMessage(templates.message(type, event));
        // Gửi sớm nhất có thể: bộ gửi thông báo (Task N5) sẽ lấy ở lần chạy kế tiếp
        notification.setScheduledAt(Instant.now());
        notification.setStatus(NotificationStatus.PENDING);

        // Lớp chống trùng 2: ràng buộc UNIQUE trên cột idempotency_key.
        // Dùng saveAndFlush thay vì save: save chỉ ghi nhớ thay đổi, đến lúc commit mới INSERT thật, khi đó lỗi
        // trùng xảy ra sau khi hàm đã return và rất khó bắt. saveAndFlush INSERT ngay, nên lỗi trùng
        // (DataIntegrityViolationException) được ném ra tại đây để LoanEventListener xử lý.
        notificationRepository.saveAndFlush(notification);
        return ReminderResult.CREATED;
    }

    @Override
    public boolean reminderExists(NotificationType type, Long loanId) {
        return notificationRepository.existsByIdempotencyKey(type.idempotencyKey(loanId));
    }
}
