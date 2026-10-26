package fu.dlms.loan.dto;

import fu.dlms.loan.entity.Loan;
import fu.dlms.loan.entity.LoanStatus;
import fu.dlms.loan.entity.LoanType;

import java.time.Instant;

public record LoanResponse(
        Long id,
        Long memberId,
        Long bookId,
        Long copyId,
        LoanType loanType,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        LoanStatus status
) {

    public static LoanResponse from(Loan loan) {
        return new LoanResponse(loan.getId(), loan.getMemberId(), loan.getBookId(), loan.getCopyId(),
                loan.getLoanType(), loan.getBorrowedAt(), loan.getDueAt(), loan.getReturnedAt(), loan.getStatus());
    }
}
