package fu.dlms.catalog.controller;

import fu.dlms.catalog.dto.req.BookRequest;
import fu.dlms.catalog.dto.res.BookResponse;
import fu.dlms.catalog.entity.BookFormat;
import fu.dlms.catalog.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping()
    public ResponseEntity<List<BookResponse>> search(@RequestParam(required = false) String q,
                                                     @RequestParam(required = false) BookFormat format) {
        return ResponseEntity.status(HttpStatus.OK).body(bookService.search(q, format));
    }



    @GetMapping("/{id}")
    public BookResponse findById(@PathVariable Long id) {
        return bookService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest request) {
        return bookService.create(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> update(@PathVariable Long id, @Valid @RequestBody BookRequest request){
        return ResponseEntity.status(HttpStatus.OK).body(bookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        bookService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // TODO: POST /{id}/reservations  - called by loan-service to check & reserve availability
}
