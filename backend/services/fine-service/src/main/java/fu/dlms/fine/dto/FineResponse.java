package fu.dlms.fine.dto;

import fu.dlms.fine.entity.Fine;
import fu.dlms.fine.entity.FineStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record FineResponse(
        Long id,
        Long loanId,
        Long memberId,
        int overdueDays,
        BigDecimal dailyRate,
        BigDecimal amount,
        FineStatus status,
        Instant calculatedAt,
        Instant paidAt
) {

    public static FineResponse from(Fine fine) {
        return new FineResponse(fine.getId(), fine.getLoanId(), fine.getMemberId(), fine.getOverdueDays(),
                fine.getDailyRate(), fine.getAmount(), fine.getStatus(), fine.getCalculatedAt(), fine.getPaidAt());
    }
}
