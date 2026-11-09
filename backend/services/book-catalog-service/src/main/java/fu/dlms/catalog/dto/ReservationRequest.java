package fu.dlms.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReservationRequest(@NotBlank @Size(max = 100) String requestKey, @NotNull Long memberId, @Size(max = 50) String barcode) {

}
