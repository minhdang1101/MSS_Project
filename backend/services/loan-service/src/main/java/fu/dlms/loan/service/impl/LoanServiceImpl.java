package fu.dlms.loan.service.impl;

import fu.dlms.loan.client.BookCatalogClient;
import fu.dlms.loan.client.MemberClient;
import fu.dlms.loan.client.dto.BookView;
import fu.dlms.loan.client.dto.MemberView;
import fu.dlms.loan.client.dto.ReservationRequest;
import fu.dlms.loan.client.dto.ReservationView;
import fu.dlms.loan.config.ClockConfig;
import fu.dlms.loan.dto.BorrowRequest;
import fu.dlms.loan.dto.LoanResponse;
import fu.dlms.loan.entity.Loan;
import fu.dlms.loan.entity.LoanStatus;
import fu.dlms.loan.entity.LoanType;
import fu.dlms.loan.exception.BusinessException;
import fu.dlms.loan.exception.ResourceNotFoundException;
import fu.dlms.loan.messaging.LoanEventType;
import fu.dlms.loan.messaging.OutboxWriter;
import fu.dlms.loan.repository.LoanRepository;
import fu.dlms.loan.service.LoanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class LoanServiceImpl implements LoanService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("borrowedAt"), Sort.Order.desc("id"));
    private static final List<LoanStatus> OPEN = List.of(LoanStatus.BORROWED, LoanStatus.OVERDUE);

    private final LoanRepository loanRepository;
    private final OutboxWriter outboxWriter;
    private final MemberClient memberClient;
    private final BookCatalogClient bookCatalogClient;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final int loanPeriodDays;

    public LoanServiceImpl(LoanRepository loanRepository, OutboxWriter outboxWriter, MemberClient memberClient,
                           BookCatalogClient bookCatalogClient, TransactionTemplate transactionTemplate, Clock clock,
                           @Value("${app.loan.default-period-days:14}") int loanPeriodDays) {
        this.loanRepository = loanRepository;
        this.outboxWriter = outboxWriter;
        this.memberClient = memberClient;
        this.bookCatalogClient = bookCatalogClient;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.loanPeriodDays = loanPeriodDays;
    }

    @Override
    public List<LoanResponse> findAll(LoanStatus status, Long memberId) {
        List<Loan> loans;
        if (status != null && memberId != null) {
            loans = loanRepository.findByMemberIdAndStatus(memberId, status, NEWEST_FIRST);
        } else if (status != null) {
            loans = loanRepository.findByStatus(status, NEWEST_FIRST);
        } else if (memberId != null) {
            loans = loanRepository.findByMemberId(memberId, NEWEST_FIRST);
        } else {
            loans = loanRepository.findAll(NEWEST_FIRST);
        }
        return loans.stream().map(LoanResponse::from).toList();
    }

    @Override
    public LoanResponse findById(Long id) {
        return LoanResponse.from(load(id));
    }

    /**
     * Borrow flow. Runs outside a transaction on purpose: the remote calls must not hold a database transaction
     * open, and the compensating release must happen only once we know the local save really failed.
     * <ol>
     *   <li>A key that was already used returns its loan (double click, network retry).</li>
     *   <li>Member must be ACTIVE (member-service).</li>
     *   <li>Book must be lendable; reserve one copy in book-catalog-service with the same key.</li>
     *   <li>Save the BORROWED loan and its LoanCreated outbox event in one transaction.</li>
     *   <li>If that save fails, release the reservation so the copy is available again.</li>
     * </ol>
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LoanResponse borrow(BorrowRequest request) {
        String key = request.idempotencyKey();
        Optional<Loan> existing = loanRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return replay(existing.get(), request);
        }

        MemberView member = memberClient.getMember(request.memberId());
        requireActive(member);
        if (loanRepository.existsByMemberIdAndBookIdAndStatusIn(request.memberId(), request.bookId(), OPEN)) {
            throw new BusinessException("Thành viên đang mượn cuốn sách này và chưa trả.");
        }
        BookView book = bookCatalogClient.getBook(request.bookId());
        requireLendable(book);

        ReservationView reservation = bookCatalogClient.reserve(request.bookId(), new ReservationRequest(key, member.id()));
        try {
            Loan saved = transactionTemplate.execute(tx -> {
                Loan loan = loanRepository.saveAndFlush(newLoan(request, book, reservation));
                outboxWriter.append(loan, LoanEventType.LOAN_CREATED);
                return loan;
            });
            log.info("Loan {} created: member {} borrowed book {} (copy {}) until {}", saved.getId(),
                    saved.getMemberId(), saved.getBookId(), saved.getCopyId(), saved.getDueAt());
            return LoanResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // Two requests with the same key raced past step 1. The winner owns the reservation (same key, so
            // catalog returned the same one): releasing it here would free the copy the winner just lent out.
            Optional<Loan> winner = loanRepository.findByIdempotencyKey(key);
            if (winner.isPresent()) {
                return replay(winner.get(), request);
            }
            releaseReservation(key);
            throw e;
        } catch (RuntimeException e) {
            releaseReservation(key);
            throw e;
        }
    }

    /**
     * Return flow: the loan becomes RETURNED once. The @Version check rejects a second, concurrent return, so
     * LoanReturned is written only once and book-catalog frees the copy exactly once; fine-service settles the fine.
     */
    @Override
    @Transactional
    public LoanResponse returnBook(Long loanId) {
        Loan loan = load(loanId);
        if (!loan.getStatus().isOpen()) {
            throw new BusinessException("Phiếu mượn này đã được trả trước đó.");
        }
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(clock.instant());
        loanRepository.saveAndFlush(loan); // version check now, before the event is written
        outboxWriter.append(loan, LoanEventType.LOAN_RETURNED);
        log.info("Loan {} returned (book {}, copy {})", loan.getId(), loan.getBookId(), loan.getCopyId());
        return LoanResponse.from(loan);
    }

    /** Due at the end of the library day, loanPeriodDays after today: what the librarian stamps on the slip. */
    Instant dueAtFor(Instant borrowedAt) {
        LocalDate dueDay = borrowedAt.atZone(ClockConfig.LIBRARY_ZONE).toLocalDate().plusDays(loanPeriodDays);
        return dueDay.atTime(23, 59, 59).atZone(ClockConfig.LIBRARY_ZONE).toInstant();
    }

    private Loan newLoan(BorrowRequest request, BookView book, ReservationView reservation) {
        Instant now = clock.instant();
        Loan loan = new Loan();
        loan.setIdempotencyKey(request.idempotencyKey());
        loan.setMemberId(request.memberId());
        loan.setBookId(request.bookId());
        loan.setCopyId(reservation != null ? reservation.copyId() : null);
        loan.setLoanType(loanTypeOf(book));
        loan.setBorrowedAt(now);
        loan.setDueAt(dueAtFor(now));
        loan.setStatus(LoanStatus.BORROWED);
        return loan;
    }

    private LoanResponse replay(Loan loan, BorrowRequest request) {
        if (!loan.getMemberId().equals(request.memberId()) || !loan.getBookId().equals(request.bookId())) {
            throw new BusinessException("Mã yêu cầu này đã được dùng cho một phiếu mượn khác.");
        }
        log.debug("Borrow request {} replayed: returning loan {}", request.idempotencyKey(), loan.getId());
        return LoanResponse.from(loan);
    }

    private static void requireActive(MemberView member) {
        String status = member.status() == null ? "" : member.status();
        switch (status) {
            case "ACTIVE" -> { }
            case "SUSPENDED" -> throw new BusinessException("Thẻ thành viên đang bị tạm khoá, không thể mượn sách.");
            case "LOCKED" -> throw new BusinessException("Thẻ thành viên đã bị khoá, không thể mượn sách.");
            default -> throw new BusinessException("Member account is not active");
        }
    }

    private static void requireLendable(BookView book) {
        if ("ARCHIVED".equals(book.status())) {
            throw new BusinessException("Sách \"" + book.title() + "\" đã ngừng cho mượn.");
        }
        if (book.availableCopies() <= 0) {
            throw new BusinessException("Sách \"" + book.title() + "\" hiện không còn bản nào để cho mượn.");
        }
    }

    private static LoanType loanTypeOf(BookView book) {
        return "DIGITAL".equalsIgnoreCase(book.format()) ? LoanType.DIGITAL : LoanType.PHYSICAL;
    }

    /** Best effort: if this fails too, the reservation stays held and catalog must expire or release it. */
    private void releaseReservation(String key) {
        try {
            bookCatalogClient.release(key);
            log.info("Reservation {} released after a failed borrow", key);
        } catch (RuntimeException e) {
            log.error("Compensating release of reservation {} failed; the copy stays held: {}", key, e.getMessage());
        }
    }

    private Loan load(Long id) {
        return loanRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Loan", id));
    }
}
