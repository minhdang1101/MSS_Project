package fu.dlms.catalog.dto;

import fu.dlms.catalog.entity.Book;
import fu.dlms.catalog.entity.BookFormat;
import fu.dlms.catalog.entity.BookStatus;

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
        BookStatus status
) {

    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getPublisher(), book.getPublicationYear(), book.getFormat(),
                book.getTotalCopies(), book.getAvailableCopies(), book.getStatus());
    }
}
