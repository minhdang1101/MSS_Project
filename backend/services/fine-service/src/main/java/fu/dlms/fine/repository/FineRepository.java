package fu.dlms.fine.repository;

import fu.dlms.fine.entity.Fine;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** The policy is fetched with every fine: a fine whose book is still out needs it to work out today's amount. */
public interface FineRepository extends JpaRepository<Fine, Long> {

    @Override
    @EntityGraph(attributePaths = "policy")
    List<Fine> findAll();

    @Override
    @EntityGraph(attributePaths = "policy")
    Optional<Fine> findById(Long id);

    @EntityGraph(attributePaths = "policy")
    Optional<Fine> findByLoanId(Long loanId);

    @EntityGraph(attributePaths = "policy")
    List<Fine> findByMemberId(Long memberId);
}
