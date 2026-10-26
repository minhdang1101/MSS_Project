package fu.dlms.member.controller;

import fu.dlms.member.dto.MemberImportResult;
import fu.dlms.member.dto.MemberRequest;
import fu.dlms.member.dto.MemberResponse;
import fu.dlms.member.dto.MemberStatusRequest;
import fu.dlms.member.exception.InvalidFileException;
import fu.dlms.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import fu.dlms.member.service.impl.MemberTemplateBuilder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final MemberService memberService;
    private final MemberTemplateBuilder templateBuilder;

    @GetMapping
    public List<MemberResponse> findAll() {
        return memberService.findAll();
    }

    /** Also used by loan-service (via Feign) to validate that a member is ACTIVE. */
    @GetMapping("/{id}")
    public MemberResponse findById(@PathVariable Long id) {
        return memberService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberRequest request) {
        return memberService.create(request);
    }

    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return memberService.update(id, request);
    }

    /** Suspend / lock / re-activate a member. Members are never deleted: loans and fines still refer to them. */
    @PatchMapping("/{id}/status")
    public MemberResponse changeStatus(@PathVariable Long id, @Valid @RequestBody MemberStatusRequest request) {
        return memberService.changeStatus(id, request.status());
    }

    /** The blank Excel file librarians fill in, then upload to {@code POST /import}. */
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(MemberTemplateBuilder.FILE_NAME).build().toString())
                .contentType(MediaType.parseMediaType(XLSX))
                .body(templateBuilder.build());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MemberImportResult importExcel(@RequestPart("file") MultipartFile file) {
        String name = file.getOriginalFilename();
        if (file.isEmpty() || name == null || !name.toLowerCase().endsWith(".xlsx")) {
            throw new InvalidFileException("Chọn một file Excel .xlsx để nhập.");
        }
        try (InputStream in = file.getInputStream()) {
            return memberService.importFromExcel(in);
        } catch (IOException e) {
            throw new InvalidFileException("Không đọc được file. Hãy dùng file Excel .xlsx.");
        }
    }
}
