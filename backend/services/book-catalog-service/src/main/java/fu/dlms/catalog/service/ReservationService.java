package fu.dlms.catalog.service;

import fu.dlms.catalog.dto.req.ReservationRequest;
import fu.dlms.catalog.dto.res.ReservationResponse;
import fu.dlms.catalog.messaging.LoanEvent;

public interface ReservationService {

    /** Takes one copy (or digital licence) for the loan. Idempotent on the request key. 404 unknown book, 409 none left. */
    ReservationResponse reserve(Long bookId, ReservationRequest request);

    /** Compensation when the loan could not be saved. An unknown or already released key does nothing. */
    void release(String requestKey);

    /** A loan came back: the copy is on the shelf again. Handling the same event twice has no further effect. */
    void onLoanReturned(LoanEvent event);
}
