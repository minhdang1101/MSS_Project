package fu.dlms.member.repository;

import fu.dlms.member.entity.Librarian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LibrarianRepository extends JpaRepository<Librarian, Long> {

    Optional<Librarian> findByStaffCode(String staffCode);
}
