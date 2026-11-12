package fu.dlms.fine.dto.res;

import fu.dlms.fine.entity.FinePolicy;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinePolicyResponse(
        Long id,
        BigDecimal dailyRate,
        int graceDays,
        BigDecimal maxAmount,
        boolean active,
        LocalDate effectiveFrom
) {

    public static FinePolicyResponse from(FinePolicy policy) {
        return new FinePolicyResponse(policy.getId(), policy.getDailyRate(), policy.getGraceDays(),
                policy.getMaxAmount(), policy.isActive(), policy.getEffectiveFrom());
    }
}
