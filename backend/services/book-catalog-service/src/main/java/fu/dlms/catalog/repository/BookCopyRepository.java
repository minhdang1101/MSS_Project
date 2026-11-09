package fu.dlms.catalog.repository;

import fu.dlms.catalog.entity.BookCopy;
import fu.dlms.catalog.entity.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    interface ShelfInfo {
        Long getBookId();

        String getShelf();
    }

    List<BookCopy> findByBookId(Long bookId);

    Optional<BookCopy> findFirstByBookIdAndStatus(Long bookId, CopyStatus status);

    /** Each book's shelf locations (one row per distinct shelf), for showing "where is it" in the list. */
    @Query("select distinct c.book.id as bookId, c.shelfLocation as shelf from BookCopy c where c.shelfLocation is not null order by c.book.id, c.shelfLocation")
    List<ShelfInfo> findShelves();
}
