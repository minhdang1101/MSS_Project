package fu.dlms.fine.service;

import fu.dlms.fine.entity.FinePolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/** Pure calculation logic - keep it free of I/O so it is easy to unit test. */
@Component
public class FineCalculator {

    public static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static LocalDate libraryDate(Instant instant) {
        return instant.atZone(LIBRARY_ZONE).toLocalDate();
    }

    /**
     * Calendar days from the due date to the day the book came back (or today while it is still out),
     * in the library's time zone, minus the days the library was closed in between.
     */
    public int lateDays(Instant dueAt, Instant until, Set<LocalDate> closedDays) {
        LocalDate due = libraryDate(dueAt);
        LocalDate end = libraryDate(until);
        if (!end.isAfter(due)) {
            return 0;
        }
        long closed = closedDays.stream().filter(day -> day.isAfter(due) && !day.isAfter(end)).count();
        return (int) (ChronoUnit.DAYS.between(due, end) - closed);
    }

    /** The daily rate for every late day beyond the grace period, never above the policy ceiling. */
    public BigDecimal amount(int lateDays, FinePolicy policy) {
        int chargeableDays = Math.max(0, lateDays - policy.getGraceDays());
        BigDecimal amount = policy.getDailyRate().multiply(BigDecimal.valueOf(chargeableDays));
        if (policy.getMaxAmount() != null && amount.compareTo(policy.getMaxAmount()) > 0) {
            return policy.getMaxAmount();
        }
        return amount;
    }
}
