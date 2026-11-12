package fu.dlms.loan.service;

import fu.dlms.loan.dto.req.BorrowRequest;
import fu.dlms.loan.dto.res.LoanResponse;
import fu.dlms.loan.entity.LoanStatus;

import java.util.List;

public interface LoanService {

    /** Newest first; both filters are optional. */
    List<LoanResponse> findAll(LoanStatus status, Long memberId);

    LoanResponse findById(Long id);

    LoanResponse borrow(BorrowRequest request);

    LoanResponse returnBook(Long loanId);
}
