package fu.dlms.catalog.dto.req;

import fu.dlms.catalog.entity.BookFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record BookRequest(
        @NotBlank @Pattern(regexp = "[0-9]{13}", message = "ISBN phải đúng 13 chữ số") String isbn,
        @NotBlank String title,
        @NotBlank String author,
        String publisher,
        @Min(value = 1000, message = "Năm xuất bản không hợp lệ") Integer publicationYear,
        @NotNull BookFormat format,
        @Min(0) int totalCopies
) {
}
