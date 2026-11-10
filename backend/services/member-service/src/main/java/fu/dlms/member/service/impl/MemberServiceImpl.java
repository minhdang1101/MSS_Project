package fu.dlms.member.service.impl;

import fu.dlms.member.dto.MemberImportResult;
import fu.dlms.member.dto.MemberImportResult.IssuedCredential;
import fu.dlms.member.dto.MemberImportResult.RowError;
import fu.dlms.member.dto.MemberImportRow;
import fu.dlms.member.dto.MemberRequest;
import fu.dlms.member.dto.MemberResponse;
import fu.dlms.member.entity.Member;
import fu.dlms.member.entity.MemberStatus;
import fu.dlms.member.exception.DuplicateValueException;
import fu.dlms.member.exception.ResourceNotFoundException;
import fu.dlms.member.keycloak.KeycloakAdminClient;
import fu.dlms.member.keycloak.KeycloakException;
import fu.dlms.member.repository.MemberRepository;
import fu.dlms.member.security.PasswordGenerator;
import fu.dlms.member.service.MemberService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The reader's profile lives in the member table; the login account (username = card number, password, role MEMBER)
 * lives in Keycloak. Every change that touches both runs in one transaction and calls Keycloak last, so when Keycloak
 * refuses (or is down) the exception rolls the database change back and the two never disagree.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private static final String MEMBER = "Member";
    private static final String NUMERIC_PHONE_MESSAGE =
            "Điện thoại: ô đang ở dạng số nên có thể đã mất số 0 đầu. Đổi cả cột sang định dạng Text rồi nhập lại";

    private final MemberRepository memberRepository;
    private final MemberExcelParser excelParser;
    private final CardNumberGenerator cardNumberGenerator;
    private final PasswordGenerator passwordGenerator;
    private final KeycloakAdminClient keycloak;
    private final Validator validator;

    @Override
    public List<MemberResponse> findAll() {
        return memberRepository.findAll().stream().map(MemberResponse::from).toList();
    }

    @Override
    public MemberResponse findById(Long id) {
        return memberRepository.findById(id)
                .map(MemberResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(MEMBER, id));
    }

    @Override
    @Transactional
    public MemberResponse create(MemberRequest request) {
        if (memberRepository.existsByEmailIgnoreCase(request.email())) {
            throw DuplicateValueException.email();
        }
        if (memberRepository.existsByPhone(request.phone())) {
            throw DuplicateValueException.phone();
        }
        // A password the library hands over: the reader must replace it at the first login
        String password = passwordGenerator.generate();
        Member member = saveWithAccount(request, password, true);
        return MemberResponse.from(member).withInitialPassword(password);
    }

    @Override
    @Transactional
    public MemberResponse register(MemberRequest request, String password) {
        if (memberRepository.existsByEmailIgnoreCase(request.email())) {
            throw DuplicateValueException.email();
        }
        if (request.phone() != null && memberRepository.existsByPhone(request.phone())) {
            throw DuplicateValueException.phone();
        }
        return MemberResponse.from(saveWithAccount(request, password, false));
    }

    @Override
    @Transactional
    public MemberResponse update(Long id, MemberRequest request) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(MEMBER, id));
        if (memberRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw DuplicateValueException.email();
        }
        if (memberRepository.existsByPhoneAndIdNot(request.phone(), id)) {
            throw DuplicateValueException.phone();
        }
        member.setFullName(request.fullName());
        member.setEmail(request.email());
        member.setPhone(request.phone());
        Member saved = memberRepository.save(member);
        keycloak.updateProfile(saved.getLibraryCardNo(), saved.getEmail(), saved.getFullName());
        return MemberResponse.from(saved);
    }

    @Override
    @Transactional
    public MemberResponse changeStatus(Long id, MemberStatus status) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(MEMBER, id));
        member.setStatus(status);
        Member saved = memberRepository.save(member);
        // A suspended reader may still sign in to see their loans and fines; only a locked card is shut out
        keycloak.setEnabled(saved.getLibraryCardNo(), status != MemberStatus.LOCKED);
        return MemberResponse.from(saved);
    }

    @Override
    @Transactional
    public MemberImportResult importFromExcel(InputStream xlsx) {
        List<MemberImportRow> rows = excelParser.parse(xlsx);
        List<MemberRequest> accepted = new ArrayList<>();
        List<Integer> acceptedRowNumbers = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        Set<String> emailsInFile = new HashSet<>();
        Set<String> phonesInFile = new HashSet<>();

        for (MemberImportRow row : rows) {
            MemberRequest request = new MemberRequest(row.fullName(), row.email(), row.phone());
            String problem = row.phoneIsNumeric() ? NUMERIC_PHONE_MESSAGE : validate(request, emailsInFile, phonesInFile);
            if (problem != null) {
                errors.add(new RowError(row.rowNumber(), problem));
                continue;
            }
            emailsInFile.add(request.email().toLowerCase(Locale.ROOT));
            phonesInFile.add(request.phone());
            accepted.add(request);
            acceptedRowNumbers.add(row.rowNumber());
        }

        if (accepted.isEmpty()) {
            return new MemberImportResult(0, errors, List.of());
        }

        // A card number and a Keycloak account are made only for rows that passed every check
        List<String> cardNumbers = cardNumberGenerator.next(accepted.size());
        List<IssuedCredential> credentials = new ArrayList<>();
        for (int i = 0; i < accepted.size(); i++) {
            String password = passwordGenerator.generate();
            Member member = memberRepository.saveAndFlush(newMember(cardNumbers.get(i), accepted.get(i)));
            try {
                keycloak.createReader(member.getLibraryCardNo(), member.getEmail(), member.getFullName(), member.getId(),
                        password, true);
            } catch (KeycloakException e) {
                // One bad row must not sink the whole file: report it and drop its profile again
                memberRepository.delete(member);
                errors.add(new RowError(acceptedRowNumbers.get(i), "Không tạo được tài khoản đăng nhập: " + e.getMessage()));
                continue;
            }
            credentials.add(new IssuedCredential(member.getLibraryCardNo(), member.getFullName(), password));
        }
        return new MemberImportResult(credentials.size(), errors, credentials);
    }

    @Override
    @Transactional
    public MemberResponse resetPassword(Long id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(MEMBER, id));
        String password = passwordGenerator.generate();
        keycloak.setPassword(member.getLibraryCardNo(), password, true);
        return MemberResponse.from(member).withInitialPassword(password);
    }

    /** Saves the profile (that gives it an id), then creates the matching login account in Keycloak. */
    private Member saveWithAccount(MemberRequest request, String password, boolean temporaryPassword) {
        Member member = memberRepository.saveAndFlush(newMember(cardNumberGenerator.next(1).get(0), request));
        keycloak.createReader(member.getLibraryCardNo(), member.getEmail(), member.getFullName(), member.getId(),
                password, temporaryPassword);
        return member;
    }

    /** Returns a human-readable reason this row cannot be imported, or null when it can. */
    private String validate(MemberRequest request, Set<String> emailsInFile, Set<String> phonesInFile) {
        Set<ConstraintViolation<MemberRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            return violations.stream().map(this::describe).sorted().collect(Collectors.joining("; "));
        }
        if (emailsInFile.contains(request.email().toLowerCase(Locale.ROOT))) {
            return "Email " + request.email() + " bị lặp trong file";
        }
        if (phonesInFile.contains(request.phone())) {
            return "Số điện thoại " + request.phone() + " bị lặp trong file";
        }
        if (memberRepository.existsByEmailIgnoreCase(request.email())) {
            return "Email " + request.email() + " đã có trong hệ thống";
        }
        if (memberRepository.existsByPhone(request.phone())) {
            return "Số điện thoại " + request.phone() + " đã có trong hệ thống";
        }
        return null;
    }

    private String describe(ConstraintViolation<MemberRequest> violation) {
        String field = switch (violation.getPropertyPath().toString()) {
            case "fullName" -> "Họ tên";
            case "email" -> "Email";
            case "phone" -> "Điện thoại";
            default -> violation.getPropertyPath().toString();
        };
        return field + ": " + violation.getMessage();
    }

    private Member newMember(String cardNo, MemberRequest request) {
        Member member = new Member();
        member.setLibraryCardNo(cardNo);
        member.setFullName(request.fullName());
        member.setEmail(request.email());
        member.setPhone(request.phone());
        member.setJoinedAt(LocalDate.now());
        return member;
    }
}
