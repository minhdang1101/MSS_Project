package fu.dlms.catalog.service.impl;

import fu.dlms.catalog.dto.req.BookCopyRequest;
import fu.dlms.catalog.dto.res.BookCopyResponse;
import fu.dlms.catalog.dto.req.BookCopyUpdateRequest;
import fu.dlms.catalog.entity.Book;
import fu.dlms.catalog.entity.BookCopy;
import fu.dlms.catalog.entity.BookFormat;
import fu.dlms.catalog.entity.BookStatus;
import fu.dlms.catalog.entity.CopyStatus;
import fu.dlms.catalog.exception.BusinessException;
import fu.dlms.catalog.exception.ResourceNotFoundException;
import fu.dlms.catalog.repository.BookCopyRepository;
import fu.dlms.catalog.repository.BookRepository;
import fu.dlms.catalog.repository.ReservationRepository;
import fu.dlms.catalog.service.BookCopyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookCopyServiceImpl implements BookCopyService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional
    public BookCopyResponse addCopy(Long bookId, BookCopyRequest request) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
        if(book.getFormat() == BookFormat.DIGITAL){
            throw new BusinessException("Digital book can not have copies");
        }
        if(book.getStatus() == BookStatus.ARCHIVED){
            throw new BusinessException("This Book Deleted");
        }
        if(bookCopyRepository.existsByBarcode(request.barcode().trim())){
            throw new BusinessException("Barcode Exist !!!!!!!");
        }
        BookCopy bookCopy = new BookCopy();
        bookCopy.setBook(book);
        bookCopy.setBarcode(request.barcode().trim());
        bookCopy.setShelfLocation(request.shelfLocation());

        book.setAvailableCopies(book.getAvailableCopies()+1);
        book.setTotalCopies(book.getTotalCopies()+1);
        book.refreshStatus();
        bookCopyRepository.save(bookCopy);
        return BookCopyResponse.from(bookCopy);

    }

    @Override
    public List<BookCopyResponse> getCopies(Long bookId) {
        bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
        return bookCopyRepository.findByBookId(bookId).stream().map(c -> BookCopyResponse.from(c)).toList();
    }

    @Override
    @Transactional
    public BookCopyResponse updateCopy(Long copyId, BookCopyUpdateRequest request) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new ResourceNotFoundException("BookCopy", copyId));

        if (isInLoanFlow(copy.getStatus())) {
            throw new BusinessException("Copy is reserved or on loan, can not be updated");
        }
        if (isInLoanFlow(request.status())) {
            throw new BusinessException("Status RESERVED and ON_LOAN are managed by the system");
        }
        if (!copy.getBarcode().equals(request.barcode()) && bookCopyRepository.existsByBarcode(request.barcode())) {
            throw new BusinessException("Barcode already exists: " + request.barcode());
        }

        Book book = copy.getBook();
        boolean wasAvailable = copy.getStatus() == CopyStatus.AVAILABLE;
        boolean willBeAvailable = request.status() == CopyStatus.AVAILABLE;
        if (wasAvailable && !willBeAvailable) {
            book.setAvailableCopies(book.getAvailableCopies() - 1);
        } else if (!wasAvailable && willBeAvailable) {
            book.setAvailableCopies(book.getAvailableCopies() + 1);
        }

        copy.setBarcode(request.barcode().trim());
        copy.setShelfLocation(request.shelfLocation());
        copy.setStatus(request.status());

        book.refreshStatus();
        return BookCopyResponse.from(copy);
    }

    @Override
    @Transactional
    public void deleteCopy(Long copyId) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new ResourceNotFoundException("BookCopy", copyId));

        if (isInLoanFlow(copy.getStatus())) {
            throw new BusinessException("Copy is reserved or on loan, can not be deleted");
        }
        if (reservationRepository.existsByCopyId(copyId)) {
            throw new BusinessException("Copy has loan history, mark it LOST or DAMAGED instead");
        }

        Book book = copy.getBook();
        book.setTotalCopies(book.getTotalCopies() - 1);
        if (copy.getStatus() == CopyStatus.AVAILABLE) {
            book.setAvailableCopies(book.getAvailableCopies() - 1);
        }
        book.refreshStatus();

        bookCopyRepository.delete(copy);
    }

    @Override
    public BookCopyResponse findByBarcode(String barcode) {
        return bookCopyRepository.findByBarcode(barcode)
                .map(BookCopyResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("BookCopy", barcode));
    }

    private boolean isInLoanFlow(CopyStatus status) {
        return status == CopyStatus.RESERVED || status == CopyStatus.ON_LOAN;
    }
}
