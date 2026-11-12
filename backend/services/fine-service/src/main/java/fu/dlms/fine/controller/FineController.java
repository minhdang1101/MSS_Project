package fu.dlms.fine.controller;

import fu.dlms.fine.dto.res.FineResponse;
import fu.dlms.fine.entity.FineStatus;
import fu.dlms.fine.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    /** My Fines (MEMBER). The member id comes from the gateway (JWT claim "mid"), never from the client. */
    @GetMapping("/me")
    public List<FineResponse> findMine(@RequestHeader("X-Member-Id") Long memberId) {
        return fineService.findByMember(memberId);
    }

    /** Fines &amp; Policy screen (LIBRARIAN). */
    @GetMapping
    public List<FineResponse> findAll(@RequestParam(required = false) FineStatus status) {
        return fineService.findAll(status);
    }

    @PatchMapping("/{id}/paid")
    public FineResponse markPaid(@PathVariable Long id) {
        return fineService.markPaid(id);
    }
}
