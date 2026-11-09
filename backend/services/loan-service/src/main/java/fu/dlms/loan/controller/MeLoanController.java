package fu.dlms.loan.controller;

import fu.dlms.loan.dto.LoanResponse;
import fu.dlms.loan.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A reader's own loans. Who the reader is comes from {@code X-Member-Id}, which the API gateway sets from the
 * verified token and overwrites on every request, so a reader can never ask for someone else's loans. (Like every
 * service, this one must not be reachable except through the gateway.)
 */
@RestController
@RequestMapping("/api/me/loans")
@RequiredArgsConstructor
public class MeLoanController {

    private final LoanService loanService;

    @GetMapping
    public List<LoanResponse> mine(@RequestHeader("X-Member-Id") Long memberId) {
        return loanService.findAll(null, memberId);
    }
}
