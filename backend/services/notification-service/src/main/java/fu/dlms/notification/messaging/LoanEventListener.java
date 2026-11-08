package fu.dlms.notification.messaging;

import fu.dlms.notification.config.RabbitMqConfig;
import fu.dlms.notification.entity.NotificationType;
import fu.dlms.notification.service.NotificationService;
import fu.dlms.notification.service.ReminderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;

/**
 * Nhận sự kiện "sắp đến hạn" (LoanDueSoon) và "quá hạn" (LoanOverdue) do loan-service phát qua RabbitMQ,
 * rồi biến mỗi sự kiện thành đúng một thông báo ở trạng thái PENDING (chờ gửi).
 *
 * <p>Class này làm 2 việc:
 * <ul>
 *   <li>Khai báo hạ tầng RabbitMQ (các hàm {@code @Bean}): queue riêng của notification-service và 2 binding
 *       nối queue với exchange "library.events". Spring tự tạo chúng trên RabbitMQ khi service khởi động.</li>
 *   <li>Lắng nghe queue (hàm {@code @RabbitListener}): mỗi message đến, Spring tự chuyển JSON thành
 *       {@link LoanEvent} và gọi {@link #onLoanEvent}.</li>
 * </ul>
 *
 * <p>Loại thông báo được quyết định bởi <b>routing key</b>, không phải trường eventType trong payload: message vào
 * được queue này chính là nhờ routing key khớp với binding nên luôn đáng tin, còn giá trị chuỗi của eventType
 * chưa được thống nhất với loan-service.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LoanEventListener {

    /** Tên queue ("hộp thư") riêng của notification-service. */
    public static final String QUEUE = "notification.loan-reminders";

    private final NotificationService notificationService;

    /** Queue bền (durable = true): RabbitMQ khởi động lại thì queue và các message đang chờ vẫn còn. */
    @Bean
    public Queue notificationQueue() {
        return new Queue(QUEUE, true);
    }

    /** Đăng ký nhận các message có routing key "loan.due-soon" (sách sắp đến hạn trả). */
    @Bean
    public Binding notificationDueSoonBinding(Queue notificationQueue, TopicExchange libraryExchange) {
        return BindingBuilder.bind(notificationQueue).to(libraryExchange).with(RabbitMqConfig.LOAN_DUE_SOON);
    }

    /** Đăng ký nhận các message có routing key "loan.overdue" (sách đã quá hạn trả). */
    @Bean
    public Binding notificationOverdueBinding(Queue notificationQueue, TopicExchange libraryExchange) {
        return BindingBuilder.bind(notificationQueue).to(libraryExchange).with(RabbitMqConfig.LOAN_OVERDUE);
    }

    /**
     * Xử lý một message lấy từ queue.
     *
     * <p>Nếu hàm chạy xong bình thường, Spring gửi ack và RabbitMQ xóa message khỏi queue. Nếu hàm ném exception,
     * message bị coi là xử lý thất bại (xem cách xử lý từng loại lỗi trong thân hàm).
     *
     * @param event      nội dung message, được Spring chuyển từ JSON sang object nhờ Jackson2JsonMessageConverter
     * @param routingKey routing key của message, được Spring lấy từ header nhờ {@code @Header(RECEIVED_ROUTING_KEY)}
     */
    @RabbitListener(queues = QUEUE)
    public void onLoanEvent(LoanEvent event, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        // Bước 1: routing key -> loại thông báo
        NotificationType type = toNotificationType(routingKey);
        if (type == null) {
            // Chỉ xảy ra khi ai đó thêm binding mới mà quên sửa hàm này.
            // Trả về bình thường (ack) để message không bị gửi lại mãi.
            log.warn("Ignoring event {} with unexpected routing key '{}'", event.eventId(), routingKey);
            return;
        }

        // Bước 2: kiểm tra dữ liệu bắt buộc; thiếu thì loại bỏ message (xem validate)
        validate(event, routingKey);

        // Bước 3: lưu thông báo, có chống trùng
        try {
            ReminderResult result = notificationService.createReminder(type, event);
            log.info("{} {} reminder for loan {} (event {})", result, type, event.loanId(), event.eventId());
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
     * <p>Message thiếu dữ liệu thì gửi lại bao nhiêu lần cũng vẫn sai. Nếu ném exception thường, Spring sẽ trả
     * message về queue và xử lý lại liên tục (vòng lặp vô hạn). {@link AmqpRejectAndDontRequeueException} báo cho
     * RabbitMQ bỏ hẳn message. Task N6 sẽ chuyển các message này vào Dead Letter Queue thay vì xóa.
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
