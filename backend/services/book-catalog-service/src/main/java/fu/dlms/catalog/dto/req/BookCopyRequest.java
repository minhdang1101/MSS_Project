package fu.dlms.catalog.dto.req;

import jakarta.validation.constraints.*;

public record BookCopyRequest(
        @NotBlank @Size(max = 50) String barcode,
        @Size(max = 50) String shelfLocation
) {

}

