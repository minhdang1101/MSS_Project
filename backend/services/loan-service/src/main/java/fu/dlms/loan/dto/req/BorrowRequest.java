package fu.dlms.loan.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Recorded by the librarian at the desk. idempotencyKey is generated once per form submit by the client, so a
 * double click or a network retry returns the same loan. The loan type follows the book's format (physical or
 * digital) and is not sent by the client.
 * barcode identifies the physical copy the librarian scanned; book-catalog requires it for a printed book and
 * ignores it for a digital one.
 */
public record BorrowRequest(
        @NotBlank @Size(max = 100) String idempotencyKey,
        @NotNull Long memberId,
        @NotNull Long bookId,
        @Size(max = 50) String barcode
) {

    /** Digital loans (and older clients) send no barcode. */
    public BorrowRequest(String idempotencyKey, Long memberId, Long bookId) {
        this(idempotencyKey, memberId, bookId, null);
    }
}
