package fu.dlms.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * notification-service: nhận sự kiện nhắc hạn từ RabbitMQ, lưu thông báo và gửi đi theo lịch.
 *
 * <ul>
 *   <li>{@code @EnableScheduling}: bật các hàm {@code @Scheduled} (NotificationDispatcher gửi thông báo định kỳ).</li>
 *   <li>{@code @EnableFeignClients}: bật các interface {@code @FeignClient} (MemberClient gọi member-service).</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
