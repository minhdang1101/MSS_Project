package fu.dlms.loan.client;

import fu.dlms.loan.client.dto.BookView;
import fu.dlms.loan.client.dto.ReservationRequest;
import fu.dlms.loan.client.dto.ReservationView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Internal calls to book-catalog-service (not routed through the gateway). Both write calls are idempotent on
 * requestKey, which is the loan's idempotency key: a retry returns the same reservation instead of a second one.
 * <ul>
 *   <li>reserve: takes one available copy (or digital licence) of the book for the loan;
 *       404 if the book does not exist, 409 if nothing is available.</li>
 *   <li>release: the compensating action when the loan could not be saved; the copy becomes available again.
 *       Releasing an unknown or already released key is a no-op.</li>
 * </ul>
 */
@FeignClient(name = "book-catalog-service", path = "/api/books", configuration = RemoteErrorConfig.class)
public interface BookCatalogClient {

    @GetMapping("/{id}")
    BookView getBook(@PathVariable("id") Long id);

    @PostMapping("/{id}/reservations")
    ReservationView reserve(@PathVariable("id") Long bookId, @RequestBody ReservationRequest request);

    @PostMapping("/reservations/{requestKey}/release")
    void release(@PathVariable("requestKey") String requestKey);
}
