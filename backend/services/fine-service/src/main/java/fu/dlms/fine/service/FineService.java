package fu.dlms.fine.service;

import fu.dlms.fine.dto.res.FineResponse;
import fu.dlms.fine.entity.FineStatus;
import fu.dlms.fine.messaging.LoanEvent;

import java.util.List;

public interface FineService {

    /** Every fine, optionally only those with the given status. Unreturned books are worked out up to now. */
    List<FineResponse> findAll(FineStatus status);

    /** The fines of one member (My Fines). */
    List<FineResponse> findByMember(Long memberId);

    /** Librarian marks a fine as paid at the desk. Only possible once the book is back. */
    FineResponse markPaid(Long fineId);

    /** Opens a fine for the loan (UNPAID, still growing). Does nothing when the loan already has a fine. */
    void onLoanOverdue(LoanEvent event);

    /** Finalizes the loan's fine, or drops it when nothing is owed. Does nothing once it is finalized. */
    void onLoanReturned(LoanEvent event);
}
