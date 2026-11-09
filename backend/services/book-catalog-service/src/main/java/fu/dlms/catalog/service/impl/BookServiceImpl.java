package fu.dlms.catalog.service.impl;

import fu.dlms.catalog.dto.BookRequest;
import fu.dlms.catalog.dto.BookResponse;
import fu.dlms.catalog.entity.Book;
import fu.dlms.catalog.entity.BookCopy;
import fu.dlms.catalog.entity.BookCover;
import fu.dlms.catalog.entity.BookFormat;
import fu.dlms.catalog.exception.BusinessException;
import fu.dlms.catalog.exception.FieldValidationException;
import fu.dlms.catalog.exception.InvalidImageException;
import fu.dlms.catalog.exception.ResourceNotFoundException;
import fu.dlms.catalog.repository.BookCopyRepository;
import fu.dlms.catalog.repository.BookCoverRepository;
import fu.dlms.catalog.repository.BookRepository;
import fu.dlms.catalog.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    static final long MAX_COVER_BYTES = 1024 * 1024;

    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final BookCoverRepository coverRepository;

    @Override
    public List<BookResponse> findAll() {
        Map<Long, Instant> covers = new HashMap<>();
        coverRepository.findAllStamps().forEach(s -> covers.put(s.getBookId(), s.getUpdatedAt()));
        Map<Long, String> shelves = shelvesByBook();
        return bookRepository.findAll().stream().map(b -> BookResponse.from(b, covers.get(b.getId()), shelves.get(b.getId()))).toList();
    }

    @Override
    public BookResponse findById(Long id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book", id));
        return BookResponse.from(book, coverStamp(id), shelvesByBook().get(id));
    }

    @Override
    @Transactional
    public BookResponse create(BookRequest request) {
        if (request.publicationYear() != null && request.publicationYear() > Year.now().getValue()) {
            throw new FieldValidationException("publicationYear", "Năm xuất bản không được ở tương lai");
        }
        String isbn = request.isbn().trim();
        if (bookRepository.existsByIsbn(isbn)) {
            throw new BusinessException("ISBN " + isbn + " đã có trong danh mục.");
        }

        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setPublisher(request.publisher() == null || request.publisher().isBlank() ? null : request.publisher().trim());
        book.setPublicationYear(request.publicationYear());
        book.setFormat(request.format());
        book.setTotalCopies(request.totalCopies());
        book.setAvailableCopies(request.totalCopies());
        book.refreshStatus();
        Book saved = bookRepository.save(book);

        // A printed book needs one row per copy, or nothing could ever be lent out.
        if (saved.getFormat() == BookFormat.PHYSICAL) {
            for (int i = 1; i <= saved.getTotalCopies(); i++) {
                BookCopy copy = new BookCopy();
                copy.setBook(saved);
                copy.setBarcode(String.format("BC-%04d-%02d", saved.getId(), i));
                copy.setShelfLocation(request.shelfLocation() == null || request.shelfLocation().isBlank()
                        ? null : request.shelfLocation().trim());
                copyRepository.save(copy);
            }
        }
        return BookResponse.from(saved, null, shelfOf(request));
    }

    @Override
    @Transactional
    public BookResponse saveCover(Long id, MultipartFile file) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book", id));
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Chưa chọn ảnh.");
        }
        if (file.getSize() > MAX_COVER_BYTES) {
            throw new InvalidImageException("Ảnh quá lớn (tối đa 1MB).");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new InvalidImageException("Không đọc được ảnh.");
        }
        String type = sniff(data); // trust the bytes, not the file name or the declared type
        if (type == null) {
            throw new InvalidImageException("Chỉ nhận ảnh JPG hoặc PNG.");
        }
        BookCover cover = coverRepository.findById(id).orElseGet(() -> {
            BookCover c = new BookCover();
            c.setBookId(id);
            return c;
        });
        cover.setContentType(type);
        cover.setData(data);
        cover.setUpdatedAt(Instant.now());
        coverRepository.save(cover);
        return BookResponse.from(book, cover.getUpdatedAt(), shelvesByBook().get(id));
    }

    @Override
    public CoverImage getCover(Long id) {
        BookCover cover = coverRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cover of book", id));
        return new CoverImage(cover.getContentType(), cover.getData());
    }

    private static void rejectFutureYear(BookRequest request) {
        if (request.publicationYear() != null && request.publicationYear() > Year.now().getValue()) {
            throw new FieldValidationException("publicationYear", "Năm xuất bản không được ở tương lai");
        }
    }

    private List<BookResponse> enrich(List<Book> books) {
        Map<Long, Instant> covers = new HashMap<>();
        coverRepository.findAllStamps().forEach(s -> covers.put(s.getBookId(), s.getUpdatedAt()));
        Map<Long, String> shelves = shelvesByBook();
        return books.stream().map(b -> BookResponse.from(b, covers.get(b.getId()), shelves.get(b.getId()))).toList();
    }

    private Map<Long, String> shelvesByBook() {
        Map<Long, String> shelves = new HashMap<>();
        copyRepository.findShelves().forEach(s -> shelves.merge(s.getBookId(), s.getShelf(), (a, b) -> a + ", " + b));
        return shelves;
    }

    private static String shelfOf(BookRequest request) {
        String shelf = request.shelfLocation();
        return request.format() == BookFormat.PHYSICAL && shelf != null && !shelf.isBlank() ? shelf.trim() : null;
    }

    private Instant coverStamp(Long id) {
        return coverRepository.findById(id).map(BookCover::getUpdatedAt).orElse(null);
    }

    /** image/jpeg or image/png judged by the first bytes; null for anything else. */
    static String sniff(byte[] d) {
        if (d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return "image/png";
        }
        return null;
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
