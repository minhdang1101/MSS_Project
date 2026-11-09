package fu.dlms.catalog.repository;

import fu.dlms.catalog.entity.BookCover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface BookCoverRepository extends JpaRepository<BookCover, Long> {

    interface CoverStamp {
        Long getBookId();

        Instant getUpdatedAt();
    }

    /** Which books have a cover and when it last changed, without loading the bytes. */
    @Query("select c.bookId as bookId, c.updatedAt as updatedAt from BookCover c")
    List<CoverStamp> findAllStamps();
}
