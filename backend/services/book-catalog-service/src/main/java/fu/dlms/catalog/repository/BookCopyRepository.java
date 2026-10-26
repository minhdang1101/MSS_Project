package fu.dlms.catalog.repository;

import fu.dlms.catalog.entity.BookCopy;
import fu.dlms.catalog.entity.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    List<BookCopy> findByBookId(Long bookId);

    Optional<BookCopy> findFirstByBookIdAndStatus(Long bookId, CopyStatus status);
}
