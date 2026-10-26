package fu.dlms.loan.dto;

import fu.dlms.loan.entity.LoanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BorrowRequest(
        @NotBlank String idempotencyKey,
        @NotNull Long memberId,
        @NotNull Long bookId,
        @NotNull LoanType loanType
) {
}
