package fu.dlms.loan.repository;

import fu.dlms.loan.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
