package fu.dlms.catalog.service;

import fu.dlms.catalog.dto.req.BookCopyRequest;
import fu.dlms.catalog.dto.res.BookCopyResponse;
import fu.dlms.catalog.dto.req.BookCopyUpdateRequest;

import java.util.List;

public interface BookCopyService {
    BookCopyResponse addCopy(Long bookId, BookCopyRequest request);

    List<BookCopyResponse> getCopies(Long bookId);

    BookCopyResponse updateCopy(Long copyId, BookCopyUpdateRequest request);

    void deleteCopy(Long copyId);

    BookCopyResponse findByBarcode(String barcode);
}
