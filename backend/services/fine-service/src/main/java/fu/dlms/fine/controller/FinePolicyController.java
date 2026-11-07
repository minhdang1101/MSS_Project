package fu.dlms.fine.controller;

import fu.dlms.fine.dto.ClosedDayRequest;
import fu.dlms.fine.dto.ClosedDayResponse;
import fu.dlms.fine.dto.FinePolicyRequest;
import fu.dlms.fine.dto.FinePolicyResponse;
import fu.dlms.fine.service.FinePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Fine rules for librarians: policy versions and the closed-day calendar. */
@RestController
@RequestMapping("/api/fine-policies")
@RequiredArgsConstructor
public class FinePolicyController {

    private final FinePolicyService finePolicyService;

    @GetMapping
    public List<FinePolicyResponse> findAll() {
        return finePolicyService.findAll();
    }

    @GetMapping("/current")
    public FinePolicyResponse findCurrent() {
        return finePolicyService.findCurrent();
    }

    /** A change of rules is a new version; fines already opened keep the version they started with. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FinePolicyResponse create(@Valid @RequestBody FinePolicyRequest request) {
        return finePolicyService.create(request);
    }

    @PostMapping("/{id}/deactivate")
    public FinePolicyResponse deactivate(@PathVariable Long id) {
        return finePolicyService.deactivate(id);
    }

    @GetMapping("/closed-days")
    public List<ClosedDayResponse> findClosedDays() {
        return finePolicyService.findClosedDays();
    }

    @PostMapping("/closed-days")
    @ResponseStatus(HttpStatus.CREATED)
    public ClosedDayResponse addClosedDay(@Valid @RequestBody ClosedDayRequest request) {
        return finePolicyService.addClosedDay(request);
    }

    @DeleteMapping("/closed-days/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeClosedDay(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        finePolicyService.removeClosedDay(date);
    }
}
