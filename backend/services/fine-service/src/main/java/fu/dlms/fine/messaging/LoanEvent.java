package fu.dlms.fine.messaging;

import java.time.Instant;

/** Payload published by loan-service on every loan state change. eventId is used for idempotent consumers. */
public record LoanEvent(
        String eventId,
        String eventType,
        Long loanId,
        Long memberId,
        Long bookId,
        Long copyId,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        Instant occurredAt
) {
}
