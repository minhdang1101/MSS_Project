package fu.dlms.catalog.service.impl;

import fu.dlms.catalog.dto.BookRequest;
import fu.dlms.catalog.dto.BookResponse;
import fu.dlms.catalog.entity.Book;
import fu.dlms.catalog.entity.BookFormat;
import fu.dlms.catalog.entity.BookStatus;
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
        if(request.format() == BookFormat.PHYSICAL){
            book.setTotalCopies(0);
            book.setAvailableCopies(0);
        } else {
            book.setTotalCopies(request.totalCopies());
            book.setAvailableCopies(request.totalCopies());
        }
        book.refreshStatus();
        return BookResponse.from(bookRepository.save(book));
    }

    @Override
    public List<BookResponse> search(String keyword) {

        if(keyword == null || keyword.isBlank()){
            keyword = "%";
        }else{
            keyword = "%" + keyword.trim().toLowerCase() +"%";
        }
        List<BookResponse> bookResponseList = bookRepository.search(keyword).stream().map(c -> BookResponse.from(c)).toList();


        return bookResponseList;
    }

    @Override
    @Transactional
    public BookResponse update(Long id, BookRequest request) {

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book",id));

        if(!book.getFormat().equals(request.format())){
            throw new BusinessException("Book can not change between digital and physical !");
        }

        if(!book.getIsbn().equals(request.isbn()) && bookRepository.existsByIsbn(request.isbn())){
            throw new BusinessException("ISBNS Exist !");
        }

        book.setIsbn(request.isbn());
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setPublisher(request.publisher());
        book.setPublicationYear(request.publicationYear());

        if(book.getFormat() == BookFormat.DIGITAL){
            int borrowed = book.getTotalCopies() - book.getAvailableCopies();
            if(request.totalCopies() < borrowed){
                throw new BusinessException("total copies must larger than the borrowed digital book");
            }else{
                book.setTotalCopies(request.totalCopies());
                book.setAvailableCopies(request.totalCopies() - borrowed);
            }
        }

        book.refreshStatus();

        return BookResponse.from(book);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("book",id));
        if(book.getStatus() == BookStatus.ARCHIVED){
            return;
        }
        if(book.getAvailableCopies() < book.getTotalCopies()){
            throw new BusinessException("Cannot archive book: some copies are on loan");
        }
        book.setStatus(BookStatus.ARCHIVED);
    }
}
