package fu.dlms.notification.messaging;

import fu.dlms.notification.config.RabbitMqConfig;
import fu.dlms.notification.entity.NotificationType;
import fu.dlms.notification.service.NotificationService;
import fu.dlms.notification.service.Recipient;
import fu.dlms.notification.service.RecipientResolver;
import fu.dlms.notification.service.ReminderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Nhận sự kiện "sắp đến hạn" (LoanDueSoon) và "quá hạn" (LoanOverdue) do loan-service phát qua RabbitMQ,
 * rồi biến mỗi sự kiện thành đúng một thông báo ở trạng thái PENDING (chờ gửi).
 *
 * <p>Queue, binding, dead letter queue và cơ chế retry được khai báo trong {@link RabbitMqConfig}. Mỗi message đến,
 * Spring tự chuyển JSON thành {@link LoanEvent} và gọi {@link #onLoanEvent}.
 *
 * <p>Loại thông báo được quyết định bởi <b>routing key</b>, không phải trường eventType trong payload: message vào
 * được queue này chính là nhờ routing key khớp với binding nên luôn đáng tin.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanEventListener {

    /** Giữ lại để code cũ / tài liệu tham chiếu; giá trị thật nằm ở RabbitMqConfig. */
    public static final String QUEUE = RabbitMqConfig.QUEUE;

    private final NotificationService notificationService;
    private final RecipientResolver recipientResolver;

    /**
     * Xử lý một message lấy từ queue.
     *
     * <p>Hàm chạy xong bình thường → Spring gửi ack, RabbitMQ xóa message. Hàm ném exception → cơ chế retry trong
     * RabbitMqConfig thử lại; hết lượt thì message được chuyển vào dead letter queue.
     *
     * @param event      nội dung message, được Spring chuyển từ JSON sang object nhờ Jackson2JsonMessageConverter
     * @param routingKey routing key của message, được Spring lấy từ header nhờ {@code @Header(RECEIVED_ROUTING_KEY)}
     */
    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void onLoanEvent(LoanEvent event, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        // Bước 1: routing key -> loại thông báo
        NotificationType type = toNotificationType(routingKey);
        if (type == null) {
            // Chỉ xảy ra khi ai đó thêm binding mới mà quên sửa hàm này.
            // Trả về bình thường (ack) để message không bị gửi lại mãi.
            log.warn("Ignoring event {} with unexpected routing key '{}'", event.eventId(), routingKey);
            return;
        }

        // Bước 2: kiểm tra dữ liệu bắt buộc; thiếu thì chuyển thẳng vào dead letter queue (xem validate)
        validate(event, routingKey);

        // Bước 3: đã có thông báo này rồi thì dừng sớm, khỏi gọi member-service
        if (notificationService.reminderExists(type, event.loanId())) {
            log.info("DUPLICATE {} reminder for loan {} (event {})", type, event.loanId(), event.eventId());
            return;
        }

        // Bước 4: hỏi member-service gửi qua email hay trong ứng dụng (Task N4).
        // Gọi ở đây, NGOÀI transaction, để không giữ kết nối database trong lúc chờ HTTP.
        Recipient recipient = recipientResolver.resolve(event.memberId());

        // Bước 5: lưu thông báo, có chống trùng
        try {
            ReminderResult result = notificationService.createReminder(type, event, recipient);
            log.info("{} {} reminder for loan {} via {} (event {})",
                    result, type, event.loanId(), recipient.channel(), event.eventId());
        } catch (DataIntegrityViolationException e) {
            // Trường hợp hiếm: 2 bản sao của cùng một sự kiện được xử lý cùng lúc. Cả hai đều kiểm tra
            // "chưa có" và cùng INSERT; database chỉ cho một bên thành công (ràng buộc UNIQUE), bên còn lại rơi
            // vào đây.
            //
            // Lỗi này được bắt ở đây (bên ngoài transaction của createReminder) chứ không bắt trong service: khi
            // database báo lỗi, Spring đánh dấu transaction "phải rollback"; nếu bắt bên trong hàm @Transactional
            // thì lúc commit Spring vẫn ném UnexpectedRollbackException.
            //
            // DataIntegrityViolationException cũng có thể do lỗi khác (vd cột NOT NULL bị trống). Vì vậy kiểm tra
            // lại: chỉ khi khóa thật sự đã có trong database mới coi là trùng; còn lại ném tiếp để không che giấu bug.
            if (!notificationService.reminderExists(type, event.loanId())) {
                throw e;
            }
            log.info("DUPLICATE {} reminder for loan {} (event {}, blocked by unique key)",
                    type, event.loanId(), event.eventId());
        }
    }

    /**
     * Đổi routing key thành loại thông báo.
     *
     * @return DUE_SOON, OVERDUE, hoặc null nếu routing key không thuộc 2 loại này
     */
    static NotificationType toNotificationType(String routingKey) {
        if (RabbitMqConfig.LOAN_DUE_SOON.equals(routingKey)) {
            return NotificationType.DUE_SOON;
        }
        if (RabbitMqConfig.LOAN_OVERDUE.equals(routingKey)) {
            return NotificationType.OVERDUE;
        }
        return null;
    }

    /**
     * Kiểm tra sự kiện có đủ loanId, memberId và dueAt (cần để tạo khóa chống trùng, biết gửi cho ai và
     * ghi hạn trả vào nội dung).
     *
     * <p>Message thiếu dữ liệu thì gửi lại bao nhiêu lần cũng vẫn sai. {@link AmqpRejectAndDontRequeueException} được
     * cấu hình là "không thử lại" (RabbitMqConfig), nên message đi thẳng vào dead letter queue để người phát triển xem
     * và sửa nguồn gửi.
     */
    private static void validate(LoanEvent event, String routingKey) {
        String missing = event.loanId() == null ? "loanId"
                : event.memberId() == null ? "memberId"
                : event.dueAt() == null ? "dueAt"
                : null;
        if (missing != null) {
            throw new AmqpRejectAndDontRequeueException(
                    "Invalid " + routingKey + " event " + event.eventId() + ": " + missing + " is missing");
        }
    }
}
