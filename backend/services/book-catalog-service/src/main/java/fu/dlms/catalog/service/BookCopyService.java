package fu.dlms.catalog.service;

import fu.dlms.catalog.dto.BookCopyRequest;
import fu.dlms.catalog.dto.BookCopyResponse;
import fu.dlms.catalog.dto.BookCopyUpdateRequest;

import java.util.List;

public interface BookCopyService {
    BookCopyResponse addCopy(Long bookId, BookCopyRequest request);

    List<BookCopyResponse> getCopies(Long bookId);

    BookCopyResponse updateCopy(Long copyId, BookCopyUpdateRequest request);

    void deleteCopy(Long copyId);

    BookCopyResponse findByBarcode(String barcode);
}
