package fu.dlms.loan.controller;

import fu.dlms.loan.dto.BorrowRequest;
import fu.dlms.loan.dto.LoanResponse;
import fu.dlms.loan.entity.LoanStatus;
import fu.dlms.loan.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Manage Loans (Screen 10): the only place where loans change. Librarian only, enforced at the gateway. */
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    /** GET /api/loans?status=OVERDUE and/or ?memberId=7 - both filters optional, newest first. */
    @GetMapping
    public List<LoanResponse> findAll(@RequestParam(required = false) LoanStatus status,
                                      @RequestParam(required = false) Long memberId) {
        return loanService.findAll(status, memberId);
    }

    @GetMapping("/{id}")
    public LoanResponse findById(@PathVariable Long id) {
        return loanService.findById(id);
    }

    /** 201 for a new loan; a replay with the same idempotencyKey returns the same loan. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse borrow(@Valid @RequestBody BorrowRequest request) {
        return loanService.borrow(request);
    }

    @PostMapping("/{id}/return")
    public LoanResponse returnBook(@PathVariable Long id) {
        return loanService.returnBook(id);
    }
}
