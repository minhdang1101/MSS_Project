package fu.dlms.fine.controller;

import fu.dlms.fine.dto.req.FinePolicyRequest;
import fu.dlms.fine.dto.res.FinePolicyResponse;
import fu.dlms.fine.service.FinePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fine Policy form (librarian): daily rate, grace days and maximum amount. */
@RestController
@RequestMapping("/api/fines/policy")
@RequiredArgsConstructor
public class FinePolicyController {

    private final FinePolicyService finePolicyService;

    @GetMapping
    public FinePolicyResponse get() {
        return finePolicyService.findCurrent();
    }

    /** A change of rules is saved as a new version; it applies to fines calculated after its effective date. */
    @PutMapping
    public FinePolicyResponse update(@Valid @RequestBody FinePolicyRequest request) {
        return finePolicyService.update(request);
    }
}
