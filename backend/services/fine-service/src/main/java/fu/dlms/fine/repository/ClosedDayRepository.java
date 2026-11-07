package fu.dlms.fine.repository;

import fu.dlms.fine.entity.ClosedDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ClosedDayRepository extends JpaRepository<ClosedDay, LocalDate> {

    List<ClosedDay> findAllByOrderByDateAsc();
}
