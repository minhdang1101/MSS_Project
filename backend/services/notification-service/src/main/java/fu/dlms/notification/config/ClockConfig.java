package fu.dlms.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Đồng hồ dùng chung. Code lấy giờ qua {@code clock.instant()} thay vì {@code Instant.now()} để test có thể truyền
 * một đồng hồ cố định (Clock.fixed) và kiểm tra chính xác thời gian. Cùng cách làm với loan-service.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
