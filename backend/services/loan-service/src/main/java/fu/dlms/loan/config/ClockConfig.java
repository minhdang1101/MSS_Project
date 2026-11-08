package fu.dlms.loan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Due dates and the overdue scan depend on "now"; tests swap in a fixed clock. */
@Configuration
public class ClockConfig {

    /** Same zone as fine-service: a book that is "due today" follows the library's calendar day. */
    public static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
