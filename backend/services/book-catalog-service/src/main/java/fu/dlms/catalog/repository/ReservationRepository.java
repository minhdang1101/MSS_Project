package fu.dlms.catalog.repository;

import fu.dlms.catalog.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByRequestKey(String requestKey);
}
