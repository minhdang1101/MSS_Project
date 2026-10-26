package fu.dlms.loan.client;

import fu.dlms.loan.client.dto.BookView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "book-catalog-service", path = "/api/books")
public interface BookCatalogClient {

    @GetMapping("/{id}")
    BookView getBook(@PathVariable("id") Long id);

    // TODO: reserve availability endpoint, e.g. POST /{id}/reservations
}
