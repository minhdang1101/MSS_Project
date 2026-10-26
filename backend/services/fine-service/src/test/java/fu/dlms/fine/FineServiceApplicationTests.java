package fu.dlms.fine;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Needs a running PostgreSQL/Eureka - enable once Testcontainers is set up")
class FineServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
