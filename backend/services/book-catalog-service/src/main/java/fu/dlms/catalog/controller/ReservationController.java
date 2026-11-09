package fu.dlms.catalog.controller;

import fu.dlms.catalog.dto.ReservationRequest;
import fu.dlms.catalog.dto.ReservationResponse;
import fu.dlms.catalog.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Internal: called by loan-service, not meant to be reached from the browser. */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/{id}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@PathVariable Long id, @Valid @RequestBody ReservationRequest request) {
        return reservationService.reserve(id, request);
    }

    @PostMapping("/reservations/{requestKey}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable String requestKey) {
        reservationService.release(requestKey);
    }
}
