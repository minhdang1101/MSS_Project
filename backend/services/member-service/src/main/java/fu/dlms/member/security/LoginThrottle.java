package fu.dlms.member.security;

import fu.dlms.member.exception.TooManyLoginAttemptsException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brake on password guessing: five wrong passwords for one staff code lock that code for five minutes.
 * Failures older than fifteen minutes stop counting. State is per instance and lost on restart, which is enough
 * for a single member-service; a shared limiter (e.g. at the gateway) would be needed to run several instances.
 * Trade-off: someone who knows a staff code can lock that librarian out for five minutes at a time.
 */
@Component
public class LoginThrottle {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_TIME = Duration.ofMinutes(5);
    static final Duration FAILURE_MEMORY = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_CODES = 10_000;

    private record Record(int failures, Instant lastFailure, Instant lockedUntil) {
    }

    private final ConcurrentHashMap<String, Record> records = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginThrottle(Clock clock) {
        this.clock = clock;
    }

    /** @throws TooManyLoginAttemptsException while the staff code is locked */
    public void checkAllowed(String staffCode) {
        Record record = records.get(key(staffCode));
        Instant now = clock.instant();
        if (record != null && record.lockedUntil() != null && now.isBefore(record.lockedUntil())) {
            long seconds = Duration.between(now, record.lockedUntil()).toSeconds();
            throw new TooManyLoginAttemptsException(Math.max(1, (seconds + 59) / 60));
        }
    }

    public void recordFailure(String staffCode) {
        Instant now = clock.instant();
        if (records.size() >= MAX_TRACKED_CODES) {
            records.entrySet().removeIf(e -> expired(e.getValue(), now));
        }
        records.compute(key(staffCode), (k, old) -> {
            int failures = (old == null || expired(old, now)) ? 1 : old.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_TIME) : null;
            return new Record(failures, now, lockedUntil);
        });
    }

    public void recordSuccess(String staffCode) {
        records.remove(key(staffCode));
    }

    private boolean expired(Record record, Instant now) {
        boolean locked = record.lockedUntil() != null && now.isBefore(record.lockedUntil());
        return !locked && now.isAfter(record.lastFailure().plus(FAILURE_MEMORY));
    }

    private String key(String staffCode) {
        return staffCode.trim().toLowerCase(Locale.ROOT);
    }
}
