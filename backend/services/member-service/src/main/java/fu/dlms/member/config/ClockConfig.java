package fu.dlms.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** One clock for the whole service, so tests can replace it with a fixed one. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
