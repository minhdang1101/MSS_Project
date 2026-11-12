package fu.dlms.loan.client.dto;

/**
 * requestKey = the loan's idempotency key, so a retried reserve never holds a second copy.
 * barcode = the scanned copy; required by book-catalog for a physical book, null for a digital one.
 */
public record ReservationRequest(String requestKey, Long memberId, String barcode) {
}
