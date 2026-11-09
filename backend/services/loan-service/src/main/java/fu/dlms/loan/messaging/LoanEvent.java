package fu.dlms.loan.messaging;

import java.time.Instant;

/**
 * Payload published by loan-service on every loan state change. eventId is used for idempotent consumers.
 * loanType (PHYSICAL or DIGITAL) lets fine-service skip digital loans, which are never fined.
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
