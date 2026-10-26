package fu.dlms.catalog.dto;

import fu.dlms.catalog.entity.BookFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Size(max = 20) String isbn,
        @NotBlank String title,
        @NotBlank String author,
        String publisher,
        Integer publicationYear,
        @NotNull BookFormat format,
        @Min(0) int totalCopies
) {
}
