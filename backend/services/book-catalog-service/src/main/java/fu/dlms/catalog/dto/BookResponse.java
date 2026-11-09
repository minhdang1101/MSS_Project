package fu.dlms.catalog.dto;

import fu.dlms.catalog.entity.Book;
import fu.dlms.catalog.entity.BookFormat;
import fu.dlms.catalog.entity.BookStatus;

import java.time.Instant;

public record BookResponse(
        Long id,
        String isbn,
        String title,
        String author,
        String publisher,
        Integer publicationYear,
        BookFormat format,
        int totalCopies,
        int availableCopies,
        BookStatus status,
        String coverUrl,
        String shelfLocation
) {

    public static BookResponse from(Book book) {
        return from(book, null, null);
    }

    /**
     * coverUrl is null when the book has no picture; the version in it makes the browser refetch a replaced one.
     * shelfLocation is where the copies sit, e.g. "A1-01" or "A1-01, A2-03"; null for a digital book.
     */
    public static BookResponse from(Book book, Instant coverUpdatedAt, String shelfLocation) {
        String coverUrl = coverUpdatedAt == null
                ? null
                : "/api/books/" + book.getId() + "/cover?v=" + coverUpdatedAt.toEpochMilli();
        return new BookResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getPublisher(), book.getPublicationYear(), book.getFormat(),
                book.getTotalCopies(), book.getAvailableCopies(), book.getStatus(), coverUrl, shelfLocation);
    }
}
