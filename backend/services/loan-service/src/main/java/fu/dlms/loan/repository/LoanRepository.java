package fu.dlms.loan.repository;

import fu.dlms.loan.entity.Loan;
import fu.dlms.loan.entity.LoanStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByIdempotencyKey(String idempotencyKey);

    List<Loan> findByMemberId(Long memberId, Sort sort);

    List<Loan> findByStatus(LoanStatus status, Sort sort);

    List<Loan> findByMemberIdAndStatus(Long memberId, LoanStatus status, Sort sort);

    boolean existsByMemberIdAndBookIdAndStatusIn(Long memberId, Long bookId, Collection<LoanStatus> statuses);

    /** Ids only: the scheduler then updates each loan in its own short transaction. */
    @Query("select l.id from Loan l where l.status = :status and l.dueAt < :time")
    List<Long> findIdsByStatusAndDueAtBefore(LoanStatus status, Instant time);

    @Query("""
            select l.id from Loan l
            where l.status = :status and l.dueSoonNotifiedAt is null and l.dueAt >= :from and l.dueAt < :to
            """)
    List<Long> findIdsDueSoonNotYetNotified(LoanStatus status, Instant from, Instant to);
}
