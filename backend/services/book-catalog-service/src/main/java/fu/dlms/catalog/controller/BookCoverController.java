package fu.dlms.catalog.controller;

import fu.dlms.catalog.dto.BookResponse;
import fu.dlms.catalog.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api/books/{id}/cover")
@RequiredArgsConstructor
public class BookCoverController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<byte[]> get(@PathVariable Long id) {
        BookService.CoverImage cover = bookService.getCover(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic()) // the URL carries a version
                .body(cover.data());
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BookResponse put(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return bookService.saveCover(id, file);
    }
}
