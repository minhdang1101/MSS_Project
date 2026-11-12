package fu.dlms.loan.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Recorded by the librarian at the desk. idempotencyKey is generated once per form submit by the client, so a
 * double click or a network retry returns the same loan. The loan type follows the book's format (physical or
 * digital) and is not sent by the client.
 */
public record BorrowRequest(
        @NotBlank @Size(max = 100) String idempotencyKey,
        @NotNull Long memberId,
        @NotNull Long bookId
) {
}
