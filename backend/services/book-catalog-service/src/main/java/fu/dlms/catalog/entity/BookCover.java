package fu.dlms.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** The picture of a book's cover, kept apart from book so listing books never loads image bytes. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "book_cover")
public class BookCover {

    @Id
    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
