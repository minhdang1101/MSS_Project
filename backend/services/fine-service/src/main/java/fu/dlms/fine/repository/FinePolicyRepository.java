package fu.dlms.fine.repository;

import fu.dlms.fine.entity.FinePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinePolicyRepository extends JpaRepository<FinePolicy, Long> {

    Optional<FinePolicy> findFirstByActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(LocalDate date);

    List<FinePolicy> findAllByOrderByEffectiveFromDescIdDesc();

    long countByActiveTrue();
}
