package fu.dlms.fine.controller;

import fu.dlms.fine.dto.FineResponse;
import fu.dlms.fine.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    @GetMapping
    public List<FineResponse> findAll(@RequestParam(required = false) Long memberId) {
        return memberId == null ? fineService.findAll() : fineService.findByMember(memberId);
    }

    @PostMapping("/{id}/pay")
    public FineResponse pay(@PathVariable Long id) {
        return fineService.pay(id);
    }

    // TODO: /api/fine-policies CRUD for librarians
}
