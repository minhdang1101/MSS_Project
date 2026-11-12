package fu.dlms.catalog.controller;

import fu.dlms.catalog.dto.req.BookCopyRequest;
import fu.dlms.catalog.dto.res.BookCopyResponse;
import fu.dlms.catalog.dto.req.BookCopyUpdateRequest;
import fu.dlms.catalog.service.BookCopyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CopyController {

    private final BookCopyService bookCopyService;

    @PostMapping("/api/books/{bookId}/copies")
    public ResponseEntity<BookCopyResponse> addCopy(@Valid @RequestBody BookCopyRequest request, @PathVariable Long bookId){
        return ResponseEntity.status(HttpStatus.CREATED).body(bookCopyService.addCopy(bookId,request));
    }

    @GetMapping("/api/books/{bookId}/copies")
    public ResponseEntity<List<BookCopyResponse>> getCopy(@PathVariable Long bookId){
        return ResponseEntity.status(HttpStatus.OK).body(bookCopyService.getCopies(bookId));
    }

    @PutMapping("/api/copies/{id}")
    public ResponseEntity<BookCopyResponse> updateCopy(@PathVariable Long id, @Valid @RequestBody BookCopyUpdateRequest request){
        return ResponseEntity.status(HttpStatus.OK).body(bookCopyService.updateCopy(id, request));
    }

    @DeleteMapping("/api/copies/{id}")
    public ResponseEntity<Void> deleteCopy(@PathVariable Long id){
        bookCopyService.deleteCopy(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/api/copies/barcode/{barcode}")
    public ResponseEntity<BookCopyResponse> findByBarcode(@PathVariable String barcode){
        return ResponseEntity.status(HttpStatus.OK).body(bookCopyService.findByBarcode(barcode));
    }
}
