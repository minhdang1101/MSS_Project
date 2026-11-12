package fu.dlms.member.service.impl;

import fu.dlms.member.dto.res.MemberImportResult;
import fu.dlms.member.dto.res.MemberImportResult.IssuedCredential;
import fu.dlms.member.dto.res.MemberImportResult.RowError;
import fu.dlms.member.dto.req.MemberImportRow;
import fu.dlms.member.dto.req.MemberRequest;
import fu.dlms.member.dto.res.MemberResponse;
import fu.dlms.member.entity.Member;
import fu.dlms.member.entity.MemberRole;
import fu.dlms.member.entity.MemberStatus;
import fu.dlms.member.exception.DuplicateValueException;
import fu.dlms.member.exception.ResourceNotFoundException;
import fu.dlms.member.repository.MemberRepository;
import fu.dlms.member.security.PasswordGenerator;
import fu.dlms.member.service.MemberService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.stream.IntStream;

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
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final Validator validator;

    @Override
    public List<MemberResponse> findAll() {
        return memberRepository.findAllByRole(MemberRole.MEMBER).stream().map(MemberResponse::from).toList();
    }

    @Override
    public MemberResponse findById(Long id) {
        return reader(id)
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
        String cardNo = cardNumberGenerator.next(1).get(0);
        Member member = newMember(cardNo, request);
        String password = passwordGenerator.generate();
        member.setPasswordHash(passwordEncoder.encode(password));
        return MemberResponse.from(memberRepository.save(member)).withInitialPassword(password);
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
        Member member = newMember(cardNumberGenerator.next(1).get(0), request);
        member.setPasswordHash(passwordEncoder.encode(password));
        return MemberResponse.from(memberRepository.save(member));
    }

    @Override
    @Transactional
    public MemberResponse update(Long id, MemberRequest request) {
        Member member = reader(id).orElseThrow(() -> new ResourceNotFoundException(MEMBER, id));
        if (memberRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw DuplicateValueException.email();
        }
        if (memberRepository.existsByPhoneAndIdNot(request.phone(), id)) {
            throw DuplicateValueException.phone();
        }
        member.setFullName(request.fullName());
        member.setEmail(request.email());
        member.setPhone(request.phone());
        return MemberResponse.from(memberRepository.save(member));
    }

    @Override
    @Transactional
    public MemberResponse changeStatus(Long id, MemberStatus status) {
        Member member = reader(id).orElseThrow(() -> new ResourceNotFoundException(MEMBER, id));
        member.setStatus(status);
        return MemberResponse.from(memberRepository.save(member));
    }

    @Override
    @Transactional
    public MemberImportResult importFromExcel(InputStream xlsx) {
        List<MemberImportRow> rows = excelParser.parse(xlsx);
        List<MemberRequest> accepted = new ArrayList<>();
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
        }

        if (accepted.isEmpty()) {
            return new MemberImportResult(0, errors, List.of());
        }

        // Card numbers are issued only for rows that will really be saved, so no number is wasted on a bad row.
        List<String> cardNumbers = cardNumberGenerator.next(accepted.size());
        List<String> passwords = new ArrayList<>();
        for (int i = 0; i < accepted.size(); i++) {
            passwords.add(passwordGenerator.generate());
        }
        // BCrypt is deliberately slow (about 0.1 s each), so hash the whole batch on all cores at once.
        List<String> hashes = IntStream.range(0, accepted.size()).parallel()
                .mapToObj(i -> passwordEncoder.encode(passwords.get(i))).toList();

        List<Member> toSave = new ArrayList<>();
        List<IssuedCredential> credentials = new ArrayList<>();
        for (int i = 0; i < accepted.size(); i++) {
            Member member = newMember(cardNumbers.get(i), accepted.get(i));
            member.setPasswordHash(hashes.get(i));
            toSave.add(member);
            credentials.add(new IssuedCredential(member.getLibraryCardNo(), member.getFullName(), passwords.get(i)));
        }
        memberRepository.saveAll(toSave);
        return new MemberImportResult(toSave.size(), errors, credentials);
    }

    @Override
    @Transactional
    public MemberResponse resetPassword(Long id) {
        Member member = reader(id).orElseThrow(() -> new ResourceNotFoundException(MEMBER, id));
        String password = passwordGenerator.generate();
        member.setPasswordHash(passwordEncoder.encode(password));
        return MemberResponse.from(memberRepository.save(member)).withInitialPassword(password);
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

    /** Only readers are managed on the Members screen: a librarian's account is never touched from it. */
    private java.util.Optional<Member> reader(Long id) {
        return memberRepository.findById(id).filter(m -> m.getRole() == MemberRole.MEMBER);
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
