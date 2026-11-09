package fu.dlms.catalog.dto;

import fu.dlms.catalog.entity.BookCopy;
import fu.dlms.catalog.entity.CopyStatus;

public record BookCopyResponse(Long id, Long bookId, String barcode, String shelfLocation, CopyStatus status) {
    public static BookCopyResponse from(BookCopy copy) {
        return new BookCopyResponse(copy.getId(), copy.getBook().getId(), copy.getBarcode(), copy.getShelfLocation(), copy.getStatus());
    }
}

