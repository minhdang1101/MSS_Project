package fu.dlms.catalog.service.impl;

import fu.dlms.catalog.dto.BookRequest;
import fu.dlms.catalog.dto.BookResponse;
import fu.dlms.catalog.entity.Book;
import fu.dlms.catalog.exception.BusinessException;
import fu.dlms.catalog.exception.ResourceNotFoundException;
import fu.dlms.catalog.repository.BookRepository;
import fu.dlms.catalog.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Override
    public List<BookResponse> findAll() {
        return bookRepository.findAll().stream().map(BookResponse::from).toList();
    }

    @Override
    public BookResponse findById(Long id) {
        return bookRepository.findById(id)
                .map(BookResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
    }

    @Override
    @Transactional
    public BookResponse create(BookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new BusinessException("ISBN already exists: " + request.isbn());
        }
        Book book = new Book();
        book.setIsbn(request.isbn());
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setPublisher(request.publisher());
        book.setPublicationYear(request.publicationYear());
        book.setFormat(request.format());
        book.setTotalCopies(request.totalCopies());
        book.setAvailableCopies(request.totalCopies());
        return BookResponse.from(bookRepository.save(book));
    }
}
