package fu.dlms.fine.service;

import fu.dlms.fine.dto.FineResponse;
import fu.dlms.fine.messaging.LoanEvent;

import java.util.List;

public interface FineService {

    /** ACCRUING fines first (their amount is worked out up to now), then the most recently calculated. */
    List<FineResponse> findAll();

    List<FineResponse> findByMember(Long memberId);

    FineResponse pay(Long fineId);

    FineResponse waive(Long fineId);

    /** Opens an ACCRUING fine for the loan. Does nothing when the loan already has a fine. */
    void onLoanOverdue(LoanEvent event);

    /** Settles the loan's fine as UNPAID, or drops it when nothing is owed. Does nothing once it is settled. */
    void onLoanReturned(LoanEvent event);
}
