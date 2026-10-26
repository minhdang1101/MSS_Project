package fu.dlms.loan.service;

import fu.dlms.loan.dto.BorrowRequest;
import fu.dlms.loan.dto.LoanResponse;

import java.util.List;

public interface LoanService {

    List<LoanResponse> findAll();

    LoanResponse findById(Long id);

    LoanResponse borrow(BorrowRequest request);

    LoanResponse returnBook(Long loanId);
}
