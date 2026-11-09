package fu.dlms.catalog.repository;

import fu.dlms.catalog.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    @Query("select b from Book b where b.status <> fu.dlms.catalog.entity.BookStatus.ARCHIVED "
            + "and (lower(b.title) like :keyword or lower(b.author) like :keyword or b.isbn like :keyword)")
    List<Book> search(@Param("keyword") String keyword);


}
