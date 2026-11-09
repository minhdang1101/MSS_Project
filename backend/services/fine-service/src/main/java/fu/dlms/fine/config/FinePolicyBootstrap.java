package fu.dlms.fine.config;

import fu.dlms.fine.entity.FinePolicy;
import fu.dlms.fine.repository.FinePolicyRepository;
import fu.dlms.fine.service.FineCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Creates the first fine policy from app.fine.default-policy.* when the table is empty, so overdue loans can be
 * fined from the first run. Later changes go through PUT /api/fines/policy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinePolicyBootstrap implements ApplicationRunner {

    private final FinePolicyRepository finePolicyRepository;
    private final Clock clock;

    @Value("${app.fine.default-policy.daily-rate:3000}")
    private BigDecimal dailyRate;

    @Value("${app.fine.default-policy.grace-days:1}")
    private int graceDays;

    /** Leave empty for no ceiling. */
    @Value("${app.fine.default-policy.max-amount:150000}")
    private BigDecimal maxAmount;

    @Override
    public void run(ApplicationArguments args) {
        if (finePolicyRepository.count() > 0) {
            return;
        }
        FinePolicy policy = new FinePolicy();
        policy.setDailyRate(dailyRate);
        policy.setGraceDays(graceDays);
        policy.setMaxAmount(maxAmount);
        policy.setActive(true);
        policy.setEffectiveFrom(FineCalculator.libraryDate(clock.instant()));
        finePolicyRepository.save(policy);
        log.info("Created initial fine policy: {}/day, {} grace day(s), ceiling {}", dailyRate, graceDays, maxAmount);
    }
}
