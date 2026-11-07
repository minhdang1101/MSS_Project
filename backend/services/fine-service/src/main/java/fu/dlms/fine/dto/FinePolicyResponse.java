package fu.dlms.fine.dto;

import fu.dlms.fine.entity.FinePolicy;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinePolicyResponse(
        Long id,
        BigDecimal dailyRate,
        int graceDays,
        BigDecimal maxAmount,
        BigDecimal lostBookFee,
        boolean active,
        LocalDate effectiveFrom
) {

    public static FinePolicyResponse from(FinePolicy policy) {
        return new FinePolicyResponse(policy.getId(), policy.getDailyRate(), policy.getGraceDays(),
                policy.getMaxAmount(), policy.getLostBookFee(), policy.isActive(), policy.getEffectiveFrom());
    }
}
