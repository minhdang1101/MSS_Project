package fu.dlms.fine.messaging;

import java.time.Instant;

/**
 * Payload published by loan-service on every loan state change. eventId is used for idempotent consumers.
 * loanType is PHYSICAL or DIGITAL; digital loans are never fined. When it is missing the loan counts as physical.
 */
public record LoanEvent(
        String eventId,
        String eventType,
        Long loanId,
        Long memberId,
        Long bookId,
        Long copyId,
        String loanType,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        Instant occurredAt
) {
}
