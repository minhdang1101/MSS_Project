package fu.dlms.fine.repository;

import fu.dlms.fine.entity.FinePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinePolicyRepository extends JpaRepository<FinePolicy, Long> {

    /** The version in force on a day: the newest one whose effective date has arrived. */
    Optional<FinePolicy> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(LocalDate date);

    List<FinePolicy> findByActiveTrue();
}
