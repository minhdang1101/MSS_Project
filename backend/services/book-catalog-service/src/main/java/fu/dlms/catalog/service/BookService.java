package fu.dlms.catalog.service;

import fu.dlms.catalog.dto.BookRequest;
import fu.dlms.catalog.dto.BookResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BookService {

    List<BookResponse> findAll();

    BookResponse findById(Long id);

    BookResponse create(BookRequest request);

    /** Books whose title, author or ISBN contains the keyword (archived ones are left out). */
    List<BookResponse> search(String keyword);

    BookResponse update(Long id, BookRequest request);

    /** Archives the book; refused while some copy is on loan. */
    void delete(Long id);

    /** Stores (or replaces) the cover picture; JPG or PNG up to 1MB. Returns the book with its new coverUrl. */
    BookResponse saveCover(Long id, MultipartFile file);

    /** The picture and its type, or 404 when the book has none. */
    CoverImage getCover(Long id);

    record CoverImage(String contentType, byte[] data) {
    }
}
