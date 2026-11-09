package fu.dlms.member.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends the reset code by e-mail through the SMTP server in spring.mail.* . When no SMTP host is configured
 * (local development) the code is written to this service's log instead, so the flow can still be tried.
 */
@Slf4j
@Component
public class ResetCodeMailer {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public ResetCodeMailer(ObjectProvider<JavaMailSender> mailSender,
                           @Value("${app.mail.from:no-reply@library.local}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(String to, String fullName, String code, int validMinutes) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP is not configured (spring.mail.host). Reset code for {} is {} (valid {} minutes)", to, code, validMinutes);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Mã đặt lại mật khẩu thư viện");
        message.setText("Xin chào " + fullName + ",\n\nMã đặt lại mật khẩu của bạn là: " + code
                + "\nMã có hiệu lực trong " + validMinutes + " phút. Không chia sẻ mã này cho bất kỳ ai.\n\n"
                + "Nếu bạn không yêu cầu, hãy bỏ qua email này.");
        sender.send(message);
    }
}
