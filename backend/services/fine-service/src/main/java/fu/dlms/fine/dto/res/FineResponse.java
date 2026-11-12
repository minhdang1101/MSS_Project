package fu.dlms.fine.dto.res;

import fu.dlms.fine.entity.Fine;
import fu.dlms.fine.entity.FineStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record FineResponse(
        Long id,
        Long loanId,
        Long memberId,
        Long bookId,
        Instant dueAt,
        Instant returnedAt,
        int overdueDays,
        BigDecimal dailyRate,
        BigDecimal amount,
        FineStatus status,
        Instant calculatedAt,
        Instant paidAt
) {

    public static FineResponse from(Fine fine) {
        return from(fine, fine.getOverdueDays(), fine.getAmount(), fine.getCalculatedAt());
    }

    /** For a fine whose book is still out: days and amount are worked out up to the moment of the request. */
    public static FineResponse from(Fine fine, int overdueDays, BigDecimal amount, Instant calculatedAt) {
        return new FineResponse(fine.getId(), fine.getLoanId(), fine.getMemberId(), fine.getBookId(),
                fine.getDueAt(), fine.getReturnedAt(), overdueDays, fine.getDailyRate(), amount,
                fine.getStatus(), calculatedAt, fine.getPaidAt());
    }
}
