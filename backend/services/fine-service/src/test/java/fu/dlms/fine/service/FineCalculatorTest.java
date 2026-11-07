package fu.dlms.fine.service;

import fu.dlms.fine.entity.FinePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FineCalculatorTest {

    private final FineCalculator calculator = new FineCalculator();

    static FinePolicy policy(int dailyRate, int graceDays, Integer maxAmount) {
        FinePolicy policy = new FinePolicy();
        policy.setDailyRate(BigDecimal.valueOf(dailyRate));
        policy.setGraceDays(graceDays);
        policy.setMaxAmount(maxAmount == null ? null : BigDecimal.valueOf(maxAmount));
        policy.setLostBookFee(BigDecimal.valueOf(20000));
        policy.setActive(true);
        policy.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        return policy;
    }

    /** 3.000đ a day, 1 grace day, 150.000đ ceiling: the rows of the librarian's fine screen. */
    @ParameterizedTest
    @CsvSource({"14, 39000", "7, 18000", "43, 126000", "4, 9000", "2, 3000", "1, 0", "0, 0", "51, 150000", "200, 150000"})
    void chargesTheDailyRateForEveryDayAfterTheGracePeriodUpToTheCeiling(int lateDays, int expected) {
        assertThat(calculator.amount(lateDays, policy(3000, 1, 150000))).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    @Test
    void noCeilingWhenMaxAmountIsEmpty() {
        assertThat(calculator.amount(100, policy(3000, 0, null))).isEqualByComparingTo("300000");
    }

    @Test
    void countsCalendarDaysInTheLibraryTimeZone() {
        // Due 10/10 23:00 in Hà Nội (16:00 UTC), back 12/10 00:30 in Hà Nội (11/10 17:30 UTC) -> 2 days, not 1
        Instant due = Instant.parse("2026-10-10T16:00:00Z");
        Instant back = Instant.parse("2026-10-11T17:30:00Z");

        assertThat(calculator.lateDays(due, back, Set.of())).isEqualTo(2);
    }

    @Test
    void returnedOnOrBeforeTheDueDateIsNotLate() {
        Instant due = Instant.parse("2026-10-10T10:00:00Z");

        assertThat(calculator.lateDays(due, Instant.parse("2026-10-10T15:00:00Z"), Set.of())).isZero();
        assertThat(calculator.lateDays(due, Instant.parse("2026-10-01T15:00:00Z"), Set.of())).isZero();
    }

    @Test
    void closedDaysBetweenTheDueDateAndTheReturnAreNotCounted() {
        Instant due = Instant.parse("2026-10-10T03:00:00Z");
        Instant back = Instant.parse("2026-10-17T03:00:00Z");
        Set<LocalDate> closed = Set.of(
                LocalDate.of(2026, 10, 10), // the due date itself: never a late day anyway
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 13),
                LocalDate.of(2026, 10, 20)); // after the return

        assertThat(calculator.lateDays(due, back, closed)).isEqualTo(5);
    }
}
