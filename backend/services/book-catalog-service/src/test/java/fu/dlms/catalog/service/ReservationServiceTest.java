package fu.dlms.catalog.service;

import fu.dlms.catalog.dto.ReservationRequest;
import fu.dlms.catalog.dto.ReservationResponse;
import fu.dlms.catalog.entity.Book;
import fu.dlms.catalog.entity.BookCopy;
import fu.dlms.catalog.entity.BookFormat;
import fu.dlms.catalog.entity.BookStatus;
import fu.dlms.catalog.entity.CopyStatus;
import fu.dlms.catalog.entity.Reservation;
import fu.dlms.catalog.entity.ReservationStatus;
import fu.dlms.catalog.exception.BusinessException;
import fu.dlms.catalog.exception.ResourceNotFoundException;
import fu.dlms.catalog.messaging.LoanEvent;
import fu.dlms.catalog.repository.BookCopyRepository;
import fu.dlms.catalog.repository.BookRepository;
import fu.dlms.catalog.repository.ProcessedEventRepository;
import fu.dlms.catalog.repository.ReservationRepository;
import fu.dlms.catalog.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceTest {

    @Mock BookRepository books;
    @Mock BookCopyRepository copies;
    @Mock ReservationRepository reservations;
    @Mock ProcessedEventRepository processed;

    ReservationServiceImpl service;
    Book book;
    BookCopy copy;

    @BeforeEach
    void setUp() {
        service = new ReservationServiceImpl(books, copies, reservations, processed);
        book = new Book();
        book.setId(5L);
        book.setTitle("Dế Mèn");
        book.setFormat(BookFormat.PHYSICAL);
        book.setTotalCopies(2);
        book.setAvailableCopies(2);
        book.setStatus(BookStatus.AVAILABLE);
        copy = new BookCopy();
        copy.setId(9L);
        copy.setBook(book);
        when(books.findById(5L)).thenReturn(Optional.of(book));
        when(copies.findFirstByBookIdAndStatus(5L, CopyStatus.AVAILABLE)).thenReturn(Optional.of(copy));
        when(reservations.findByRequestKey(any())).thenReturn(Optional.empty());
        when(reservations.save(any(Reservation.class))).thenAnswer(i -> {
            Reservation r = i.getArgument(0);
            r.setId(77L);
            return r;
        });
    }

    @Test
    void reservesAPrintedCopy() {
        ReservationResponse r = service.reserve(5L, new ReservationRequest("k1", 1L));

        assertThat(r.copyId()).isEqualTo(9L);
        assertThat(r.status()).isEqualTo(ReservationStatus.HELD);
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.ON_LOAN);
        assertThat(book.getAvailableCopies()).isEqualTo(1);
    }

    @Test
    void digitalBookTakesALicenceAndNoCopy() {
        book.setFormat(BookFormat.DIGITAL);

        ReservationResponse r = service.reserve(5L, new ReservationRequest("k1", 1L));

        assertThat(r.copyId()).isNull();
        assertThat(book.getAvailableCopies()).isEqualTo(1);
    }

    @Test
    void retryWithTheSameKeyReturnsTheSameHold() {
        Reservation held = new Reservation();
        held.setId(3L);
        held.setBook(book);
        held.setCopy(copy);
        when(reservations.findByRequestKey("k1")).thenReturn(Optional.of(held));

        ReservationResponse r = service.reserve(5L, new ReservationRequest("k1", 1L));

        assertThat(r.id()).isEqualTo(3L);
        assertThat(book.getAvailableCopies()).isEqualTo(2);
    }

    @Test
    void nothingLeftIsA409() {
        book.setAvailableCopies(0);
        assertThatThrownBy(() -> service.reserve(5L, new ReservationRequest("k1", 1L))).isInstanceOf(BusinessException.class);
    }

    @Test
    void archivedBookCannotBeBorrowed() {
        book.setStatus(BookStatus.ARCHIVED);
        assertThatThrownBy(() -> service.reserve(5L, new ReservationRequest("k1", 1L))).isInstanceOf(BusinessException.class);
    }

    @Test
    void unknownBookIsA404() {
        assertThatThrownBy(() -> service.reserve(6L, new ReservationRequest("k1", 1L))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void releaseGivesTheCopyBackOnce() {
        service.reserve(5L, new ReservationRequest("k1", 1L));
        Reservation held = new Reservation();
        held.setBook(book);
        held.setCopy(copy);
        when(reservations.findByRequestKey("k1")).thenReturn(Optional.of(held));

        service.release("k1");
        service.release("k1"); // already RELEASED: no second increment

        assertThat(book.getAvailableCopies()).isEqualTo(2);
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
        assertThat(held.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void returnedLoanPutsTheCopyBackAndIsHandledOnce() {
        book.setAvailableCopies(1);
        copy.setStatus(CopyStatus.ON_LOAN);
        when(copies.findById(9L)).thenReturn(Optional.of(copy));
        LoanEvent event = new LoanEvent("e1", "LoanReturned", 1L, 1L, 5L, 9L, null, null, null, null);

        service.onLoanReturned(event);

        assertThat(book.getAvailableCopies()).isEqualTo(2);
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);

        when(processed.existsById(any())).thenReturn(true);
        service.onLoanReturned(event);
        assertThat(book.getAvailableCopies()).isEqualTo(2);
    }

    @Test
    void countNeverExceedsTotal() {
        when(copies.findById(9L)).thenReturn(Optional.of(copy));
        service.onLoanReturned(new LoanEvent("e2", "LoanReturned", 1L, 1L, 5L, 9L, null, null, null, null));
        assertThat(book.getAvailableCopies()).isEqualTo(2);
        verify(processed).save(any());
    }

    @Test
    void releaseOfUnknownKeyDoesNothing() {
        service.release("nope");
        verify(reservations, never()).save(any());
    }
}
