package fu.dlms.catalog.service;

import fu.dlms.catalog.dto.BookRequest;
import fu.dlms.catalog.dto.BookResponse;

import java.util.List;

public interface BookService {

    List<BookResponse> findAll();

    BookResponse findById(Long id);

    BookResponse create(BookRequest request);
}
