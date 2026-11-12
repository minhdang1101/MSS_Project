package fu.dlms.catalog.dto.res;

import fu.dlms.catalog.entity.Reservation;
import fu.dlms.catalog.entity.ReservationStatus;


public record ReservationResponse(Long id, Long bookId, Long copyId, ReservationStatus status) {

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(r.getId(), r.getBook().getId(),
                r.getCopy() == null ? null : r.getCopy().getId(), r.getStatus());
    }
}
