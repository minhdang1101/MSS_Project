package fu.dlms.fine.service;

import fu.dlms.fine.entity.FinePolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Pure calculation logic - keep it free of I/O so it is easy to unit test. */
@Component
public class FineCalculator {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Days late after the grace period, counted in calendar days in the library's time zone. */
    public int overdueDays(Instant dueAt, Instant returnedAt, FinePolicy policy) {
        long late = ChronoUnit.DAYS.between(
                dueAt.atZone(LIBRARY_ZONE).toLocalDate(),
                returnedAt.atZone(LIBRARY_ZONE).toLocalDate());
        return (int) Math.max(0, late - policy.getGraceDays());
    }

    public BigDecimal amount(int overdueDays, FinePolicy policy) {
        BigDecimal amount = policy.getDailyRate().multiply(BigDecimal.valueOf(overdueDays));
        if (policy.getMaxAmount() != null && amount.compareTo(policy.getMaxAmount()) > 0) {
            return policy.getMaxAmount();
        }
        return amount;
    }
}
