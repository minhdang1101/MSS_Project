package fu.dlms.catalog.dto;

import fu.dlms.catalog.entity.Reservation;
import fu.dlms.catalog.entity.ReservationStatus;

/** copyId is null for a digital book: there is no physical copy to hand out. */
public record ReservationResponse(Long id, Long bookId, Long copyId, ReservationStatus status) {

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(r.getId(), r.getBook().getId(),
                r.getCopy() == null ? null : r.getCopy().getId(), r.getStatus());
    }
}
