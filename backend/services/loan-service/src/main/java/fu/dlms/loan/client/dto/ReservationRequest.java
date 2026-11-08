package fu.dlms.loan.client.dto;

/** requestKey = the loan's idempotency key, so a retried reserve never holds a second copy. */
public record ReservationRequest(String requestKey, Long memberId) {
}
