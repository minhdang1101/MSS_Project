package fu.dlms.fine.repository;

import fu.dlms.fine.entity.Fine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {

    Optional<Fine> findByLoanId(Long loanId);

    List<Fine> findByMemberId(Long memberId);
}
