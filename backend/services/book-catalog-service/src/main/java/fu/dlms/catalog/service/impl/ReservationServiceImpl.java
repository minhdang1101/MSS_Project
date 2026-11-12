package fu.dlms.catalog.service.impl;

import fu.dlms.catalog.dto.req.ReservationRequest;
import fu.dlms.catalog.dto.res.ReservationResponse;
import fu.dlms.catalog.entity.Book;
import fu.dlms.catalog.entity.BookCopy;
import fu.dlms.catalog.entity.BookFormat;
import fu.dlms.catalog.entity.BookStatus;
import fu.dlms.catalog.entity.CopyStatus;
import fu.dlms.catalog.entity.ProcessedEvent;
import fu.dlms.catalog.entity.Reservation;
import fu.dlms.catalog.entity.ReservationStatus;
import fu.dlms.catalog.exception.BusinessException;
import fu.dlms.catalog.exception.FieldValidationException;
import fu.dlms.catalog.exception.ResourceNotFoundException;
import fu.dlms.catalog.messaging.LoanEvent;
import fu.dlms.catalog.repository.BookCopyRepository;
import fu.dlms.catalog.repository.BookRepository;
import fu.dlms.catalog.repository.ProcessedEventRepository;
import fu.dlms.catalog.repository.ReservationRepository;
import fu.dlms.catalog.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    static final String CONSUMER = "catalog.loan-returned";
    private static final Duration HOLD = Duration.ofMinutes(15);

    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final ReservationRepository reservationRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Override
    @Transactional
    public ReservationResponse reserve(Long bookId, ReservationRequest request) {
        Optional<Reservation> existing = reservationRepository.findByRequestKey(request.requestKey());
        if (existing.isPresent()) {
            return ReservationResponse.from(existing.get());
        }
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
        if (book.getStatus() == BookStatus.ARCHIVED) {
            throw new BusinessException("Sách \"" + book.getTitle() + "\" hiện không được phép cho mượn.");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new BusinessException("Sách \"" + book.getTitle() + "\" đã hết, không còn bản nào để mượn.");
        }

        BookCopy copy = null;
        if (book.getFormat() == BookFormat.PHYSICAL) {
            if(request.barcode() == null || request.barcode().isBlank()){
                throw new FieldValidationException("barcode", "Sách vật lý phải quét barcode") ;
            }
            copy = copyRepository.findByBarcode(request.barcode().trim()).orElseThrow(() -> new ResourceNotFoundException("Book Copy", request.barcode()));
            if(!copy.getBook().getId().equals(bookId)){
                throw new BusinessException(request.barcode() + " không khớp với Book ID: " + bookId);
            }
            if(!(copy.getStatus() == CopyStatus.AVAILABLE)){
                throw new BusinessException("Cuốn sách này hiện không khả dụng");
            }
            copy.setStatus(CopyStatus.ON_LOAN);
        }
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        book.refreshStatus();

        Reservation reservation = new Reservation();
        reservation.setBook(book);
        reservation.setCopy(copy);
        reservation.setRequestKey(request.requestKey());
        reservation.setStatus(ReservationStatus.HELD);
        reservation.setExpiresAt(Instant.now().plus(HOLD));
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public void release(String requestKey) {
        reservationRepository.findByRequestKey(requestKey)
                .filter(r -> r.getStatus() == ReservationStatus.HELD)
                .ifPresent(r -> {
                    putBack(r.getBook(), r.getCopy());
                    r.setStatus(ReservationStatus.RELEASED);
                    log.info("Reservation {} released", requestKey);
                });

        reservationRepository.findByRequestKey(requestKey);

    }

    @Override
    @Transactional
    public void onLoanReturned(LoanEvent event) {
        ProcessedEvent.Key key = new ProcessedEvent.Key(event.eventId(), CONSUMER);
        if (processedEventRepository.existsById(key)) {
            return;
        }
        Book book = bookRepository.findById(event.bookId()).orElse(null);
        if (book == null) {
            log.warn("Loan {} returned a book {} that no longer exists", event.loanId(), event.bookId());
        } else {
            BookCopy copy = event.copyId() == null ? null : copyRepository.findById(event.copyId()).orElse(null);
            putBack(book, copy);
        }
        processedEventRepository.save(new ProcessedEvent(key));
    }

    private void putBack(Book book, BookCopy copy) {
        if (copy != null) {
            copy.setStatus(CopyStatus.AVAILABLE);
        }
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        book.refreshStatus();
    }
}
