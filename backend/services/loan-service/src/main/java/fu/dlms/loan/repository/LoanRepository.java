package fu.dlms.loan.repository;

import fu.dlms.loan.entity.Loan;
import fu.dlms.loan.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByIdempotencyKey(String idempotencyKey);

    List<Loan> findByMemberId(Long memberId);

    List<Loan> findByStatusAndDueAtBefore(LoanStatus status, Instant time);

    List<Loan> findByStatusAndDueAtBetween(LoanStatus status, Instant from, Instant to);
}
