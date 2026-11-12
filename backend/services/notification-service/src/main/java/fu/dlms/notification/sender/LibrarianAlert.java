package fu.dlms.notification.sender;

import fu.dlms.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Báo cho thủ thư khi một thông báo gửi hỏng hẳn (đã FAILED sau mọi lần thử), để họ không phải chờ tình cờ mở chuông
 * trên màn Nhắc hạn mới biết.
 *
 * <p>Người nhận lấy từ {@code app.notification.alert.librarian-emails} (nhiều địa chỉ cách nhau bằng dấu phẩy). Để
 * trống thì chỉ ghi log. Thư đi qua cùng máy chủ SMTP với thông báo thường, nên chỉ gửi thật khi
 * {@code app.notification.mail.enabled=true}.
 *
 * <p>Cảnh báo lỗi (SMTP đang hỏng thì chính thư cảnh báo cũng có thể hỏng) không bao giờ được làm hỏng việc xử lý
 * thông báo, nên mọi lỗi ở đây chỉ được ghi log, không ném ra ngoài.
 */
@Slf4j
@Component
public class LibrarianAlert {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean mailEnabled;
    private final String from;
    private final List<String> librarians;

    public LibrarianAlert(ObjectProvider<JavaMailSender> mailSender,
                          @Value("${app.notification.mail.enabled:false}") boolean mailEnabled,
                          @Value("${app.notification.mail.from:thuvien@dlms.local}") String from,
                          @Value("${app.notification.alert.librarian-emails:}") String librarianEmails) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.from = from;
        this.librarians = Arrays.stream(librarianEmails.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Gọi khi {@code notification} vừa chuyển sang FAILED. Không bao giờ ném lỗi. */
    public void notificationFailed(Notification notification) {
        String subject = "[Thư viện] Thông báo #" + notification.getId() + " gửi lỗi";
        String text = describe(notification);
        if (librarians.isEmpty()) {
            log.warn("{} (chưa cấu hình app.notification.alert.librarian-emails nên không gửi cảnh báo): {}",
                    subject, notification.getLastError());
            return;
        }
        if (!mailEnabled) {
            log.info("[LIBRARIAN ALERT simulated] to {} | {} | {}", librarians, subject, text);
            return;
        }
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null) {
                log.warn("Không gửi được cảnh báo thủ thư: chưa cấu hình spring.mail.host");
                return;
            }
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(librarians.toArray(String[]::new));
            mail.setSubject(subject);
            mail.setText(text);
            sender.send(mail);
            log.info("Đã gửi cảnh báo thông báo #{} lỗi tới thủ thư {}", notification.getId(), librarians);
        } catch (RuntimeException e) {
            log.error("Không gửi được cảnh báo thủ thư về thông báo #{}: {}", notification.getId(), e.getMessage());
        }
    }

    private static String describe(Notification n) {
        return "Một thông báo gửi cho bạn đọc không gửi được sau " + n.getAttempts() + " lần thử.\n\n"
                + "Thông báo: #" + n.getId() + " (" + n.getType() + ", kênh " + n.getChannel() + ")\n"
                + "Bạn đọc (mã thành viên): " + n.getMemberId() + "\n"
                + "Người nhận: " + n.getRecipient() + "\n"
                + "Tiêu đề: " + n.getSubject() + "\n"
                + "Lý do lỗi: " + n.getLastError() + "\n\n"
                + "Cách xử lý: mở màn \"Nhắc hạn\" (chuông \"Thông báo gửi lỗi\"), sửa nguyên nhân rồi bấm \"Gửi lại\".";
    }
}
