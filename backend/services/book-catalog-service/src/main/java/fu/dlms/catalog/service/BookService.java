package fu.dlms.catalog.service;

import fu.dlms.catalog.dto.BookRequest;
import fu.dlms.catalog.dto.BookResponse;
import fu.dlms.catalog.entity.BookFormat;

import java.util.List;

public interface BookService {

    BookResponse findById(Long id);

    BookResponse create(BookRequest request);

    List<BookResponse> search(String q, BookFormat format);

    BookResponse update(Long id, BookRequest request);

    void delete(Long id);
}
