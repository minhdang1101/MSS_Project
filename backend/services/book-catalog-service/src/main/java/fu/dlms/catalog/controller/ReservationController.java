package fu.dlms.catalog.controller;

import fu.dlms.catalog.dto.req.ReservationRequest;
import fu.dlms.catalog.dto.res.ReservationResponse;
import fu.dlms.catalog.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/{id}/reservations")
    public ResponseEntity<ReservationResponse> reserve(@PathVariable Long id, @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.reserve(id, request));
    }

    @PostMapping("/reservations/{requestKey}/release")
    public ResponseEntity<Void> release(@PathVariable String requestKey) {
        reservationService.release(requestKey);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
