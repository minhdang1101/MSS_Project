package fu.dlms.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Sent by loan-service. requestKey is the loan's idempotency key: the same key never holds a second copy. */
public record ReservationRequest(@NotBlank @Size(max = 100) String requestKey, @NotNull Long memberId) {
}
