package fu.dlms.notification.sender;

import fu.dlms.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Gửi một thông báo ra ngoài theo kênh của nó (Task N5).
 *
 * <ul>
 *   <li><b>EMAIL</b>: nếu {@code app.notification.mail.enabled=true} thì gửi thật qua SMTP (khi chạy Docker là Mailpit,
 *       một hộp thư giả có giao diện web ở http://localhost:8025). Nếu tắt (mặc định khi chạy từ IDE) thì chỉ ghi log,
 *       để chạy được mà không cần mail server.</li>
 *   <li><b>SMS</b>: dự án chưa có nhà cung cấp SMS nên chỉ ghi log.</li>
 *   <li><b>IN_APP</b>: không cần gửi đi đâu, độc giả đọc trong ứng dụng qua GET /api/notifications/me.</li>
 * </ul>
 *
 * <p>Gửi lỗi thì ném exception; NotificationDelivery sẽ ghi lại lỗi và hẹn gửi lại.
 */
@Slf4j
@Component
public class NotificationSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean mailEnabled;
    private final String from;

    /**
     * JavaMailSender chỉ được Spring tạo khi có cấu hình spring.mail.host, nên lấy qua ObjectProvider (có thể không có)
     * thay vì inject trực tiếp (sẽ lỗi khởi động khi chạy từ IDE không có mail server).
     */
    public NotificationSender(ObjectProvider<JavaMailSender> mailSender,
                              @Value("${app.notification.mail.enabled:false}") boolean mailEnabled,
                              @Value("${app.notification.mail.from:thuvien@dlms.local}") String from) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.from = from;
    }

    public void send(Notification notification) {
        switch (notification.getChannel()) {
            case EMAIL -> sendEmail(notification);
            case SMS -> log.info("[SMS simulated] to {}: {}", notification.getRecipient(), notification.getMessage());
            case IN_APP -> log.info("[IN_APP] notification {} is now visible to member {} in the app",
                    notification.getId(), notification.getMemberId());
        }
    }

    private void sendEmail(Notification notification) {
        if (!mailEnabled) {
            log.info("[EMAIL simulated] to {} | {} | {}",
                    notification.getRecipient(), notification.getSubject(), notification.getMessage());
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("app.notification.mail.enabled=true but spring.mail.host is not configured");
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(notification.getRecipient());
        mail.setSubject(notification.getSubject());
        mail.setText(notification.getMessage());
        sender.send(mail); // ném MailException nếu SMTP lỗi
        log.info("[EMAIL] sent notification {} to {}", notification.getId(), notification.getRecipient());
    }
}
