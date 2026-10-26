package fu.dlms.member.service;

import fu.dlms.member.exception.TooManyLoginAttemptsException;
import fu.dlms.member.security.LoginThrottle;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginThrottleTest {

    /** A clock the test can move forward. */
    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-10-26T08:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MovableClock clock = new MovableClock();
    private final LoginThrottle throttle = new LoginThrottle(clock);

    private void failTimes(String code, int n) {
        for (int i = 0; i < n; i++) throttle.recordFailure(code);
    }

    @Test
    void fourFailuresStillAllowAnAttempt() {
        failTimes("TT-1", 4);

        assertThatCode(() -> throttle.checkAllowed("TT-1")).doesNotThrowAnyException();
    }

    @Test
    void fifthFailureLocksTheStaffCodeAndTheMessageSaysForHowLong() {
        failTimes("TT-1", 5);

        assertThatThrownBy(() -> throttle.checkAllowed("TT-1"))
                .isInstanceOfSatisfying(TooManyLoginAttemptsException.class, e -> {
                    assertThat(e.getRetryAfterMinutes()).isEqualTo(5);
                    assertThat(e.getMessage()).isEqualTo("Đăng nhập sai quá nhiều lần. Thử lại sau 5 phút.");
                });
    }

    @Test
    void lockExpiresAfterFiveMinutes() {
        failTimes("TT-1", 5);
        clock.advance(Duration.ofMinutes(5).plusSeconds(1));

        assertThatCode(() -> throttle.checkAllowed("TT-1")).doesNotThrowAnyException();
    }

    @Test
    void successClearsTheFailureCount() {
        failTimes("TT-1", 4);
        throttle.recordSuccess("TT-1");
        failTimes("TT-1", 4);

        assertThatCode(() -> throttle.checkAllowed("TT-1")).doesNotThrowAnyException();
    }

    @Test
    void failuresOlderThanFifteenMinutesNoLongerCount() {
        failTimes("TT-1", 4);
        clock.advance(Duration.ofMinutes(16));
        throttle.recordFailure("TT-1");

        assertThatCode(() -> throttle.checkAllowed("TT-1")).doesNotThrowAnyException();
    }

    @Test
    void staffCodesAreCountedIndependentlyAndIgnoringCase() {
        failTimes("tt-1", 5);

        assertThatThrownBy(() -> throttle.checkAllowed("TT-1")).isInstanceOf(TooManyLoginAttemptsException.class);
        assertThatCode(() -> throttle.checkAllowed("TT-2")).doesNotThrowAnyException();
    }
}
