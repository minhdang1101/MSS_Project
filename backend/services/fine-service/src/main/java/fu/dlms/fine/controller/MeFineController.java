package fu.dlms.fine.controller;

import fu.dlms.fine.dto.FineResponse;
import fu.dlms.fine.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** A reader's own fines; the member comes from the gateway-set {@code X-Member-Id}, never from the request. */
@RestController
@RequestMapping("/api/me/fines")
@RequiredArgsConstructor
public class MeFineController {

    private final FineService fineService;

    @GetMapping
    public List<FineResponse> mine(@RequestHeader("X-Member-Id") Long memberId) {
        return fineService.findByMember(memberId);
    }
}
