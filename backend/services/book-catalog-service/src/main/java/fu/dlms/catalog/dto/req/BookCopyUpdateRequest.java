package fu.dlms.catalog.dto.req;

import fu.dlms.catalog.entity.CopyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookCopyUpdateRequest(
        @NotBlank @Size(max = 50) String barcode,
        @Size(max = 50) String shelfLocation,
        @NotNull CopyStatus status
) {
}
