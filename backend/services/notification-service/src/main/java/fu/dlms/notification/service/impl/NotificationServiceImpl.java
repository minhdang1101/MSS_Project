package fu.dlms.notification.service.impl;

import fu.dlms.notification.dto.NotificationResponse;
import fu.dlms.notification.entity.Notification;
import fu.dlms.notification.entity.NotificationStatus;
import fu.dlms.notification.entity.NotificationType;
import fu.dlms.notification.exception.BusinessException;
import fu.dlms.notification.exception.ResourceNotFoundException;
import fu.dlms.notification.messaging.LoanEvent;
import fu.dlms.notification.repository.NotificationRepository;
import fu.dlms.notification.service.NotificationService;
import fu.dlms.notification.service.NotificationTemplates;
import fu.dlms.notification.service.Recipient;
import fu.dlms.notification.service.ReminderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * Cài đặt nghiệp vụ thông báo.
 *
 * <p>{@code @Transactional(readOnly = true)} ở mức class: mặc định mọi hàm chỉ đọc dữ liệu. Hàm nào cần ghi thì
 * khai báo lại {@code @Transactional} riêng.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplates templates;
    private final Clock clock;

    @Override
    public List<NotificationResponse> findAll(Long memberId, NotificationStatus status) {
        return notificationRepository.search(memberId, status).stream().map(NotificationResponse::from).toList();
    }

    @Override
    public NotificationResponse findById(Long id) {
        return NotificationResponse.from(load(id));
    }

    @Override
    @Transactional
    public NotificationResponse retry(Long id) {
        Notification notification = load(id);
        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new BusinessException("Chỉ gửi lại được thông báo đang ở trạng thái FAILED (hiện tại: "
                    + notification.getStatus() + ")");
        }
        notification.setStatus(NotificationStatus.PENDING);
        notification.setAttempts(0);
        notification.setLastError(null);
        notification.setScheduledAt(clock.instant());
        return NotificationResponse.from(notification);
    }

    @Override
    public List<NotificationResponse> findMine(Long memberId) {
        return notificationRepository.findByMemberIdOrderByScheduledAtDesc(memberId).stream()
                .map(NotificationResponse::from).toList();
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long id, Long memberId) {
        Notification notification = notificationRepository.findById(id)
                .filter(n -> Objects.equals(n.getMemberId(), memberId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (notification.getReadAt() == null) {
            notification.setReadAt(clock.instant());
        }
        // Không cần gọi save: entity đang được JPA quản lý, thay đổi tự được ghi khi transaction commit
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public ReminderResult createReminder(NotificationType type, LoanEvent event, Recipient recipient) {
        // Khóa chống trùng, ví dụ "DUE_SOON:15" (xem NotificationType.idempotencyKey)
        String key = type.idempotencyKey(event.loanId());

        // Lớp chống trùng 1: hỏi database trước. Chặn được gần như mọi trường hợp gửi lại
        // (RabbitMQ giao lại, outbox của loan-service gửi lại...).
        if (notificationRepository.existsByIdempotencyKey(key)) {
            return ReminderResult.DUPLICATE;
        }

        Notification notification = new Notification();
        notification.setIdempotencyKey(key);
        notification.setSourceEventId(event.eventId()); // chỉ để truy vết sự kiện gốc
        notification.setMemberId(event.memberId());
        notification.setLoanId(event.loanId());
        notification.setType(type);
        notification.setChannel(recipient.channel());
        notification.setRecipient(recipient.address());
        notification.setSubject(templates.subject(type));
        notification.setMessage(templates.message(type, event));
        // Gửi sớm nhất có thể: NotificationDispatcher sẽ lấy ở lần quét kế tiếp
        notification.setScheduledAt(clock.instant());
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

    private Notification load(Long id) {
        return notificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", id));
    }
}
