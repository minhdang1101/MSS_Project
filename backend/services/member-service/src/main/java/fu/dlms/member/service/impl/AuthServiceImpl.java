package fu.dlms.member.service.impl;

import fu.dlms.member.dto.req.ChangePasswordRequest;
import fu.dlms.member.dto.req.ForgotPasswordRequest;
import fu.dlms.member.dto.req.LoginRequest;
import fu.dlms.member.dto.res.LoginResponse;
import fu.dlms.member.dto.req.MemberRequest;
import fu.dlms.member.dto.res.MemberResponse;
import fu.dlms.member.dto.req.RegisterRequest;
import fu.dlms.member.dto.req.ResetPasswordRequest;
import fu.dlms.member.dto.req.VerifyResetCodeRequest;
import fu.dlms.member.entity.Member;
import fu.dlms.member.entity.MemberRole;
import fu.dlms.member.entity.MemberStatus;
import fu.dlms.member.exception.BusinessException;
import fu.dlms.member.exception.InvalidCredentialsException;
import fu.dlms.member.repository.MemberRepository;
import fu.dlms.member.security.IssuedToken;
import fu.dlms.member.security.JwtService;
import fu.dlms.member.security.LoginThrottle;
import fu.dlms.member.security.ResetCodeMailer;
import fu.dlms.member.service.AuthService;
import fu.dlms.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Deliberately not @Transactional: each repository call opens its own short transaction, so a database connection
 * is not held while the slow BCrypt comparison runs.
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    static final String LIBRARIAN = "LIBRARIAN";
    static final String MEMBER = "MEMBER";
    /** BCrypt only looks at the first 72 bytes, so a longer password would silently match its own prefix. */
    private static final int BCRYPT_MAX_BYTES = 72;
    static final int CODE_VALID_MINUTES = 10;
    static final int MAX_CODE_ATTEMPTS = 5;
    /** Asking for a new code within this time of the last one does nothing, so the form cannot be used to spam an inbox. */
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final String BAD_CODE = "Mã không đúng hoặc đã hết hạn.";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginThrottle throttle;
    private final MemberService memberService;
    private final ResetCodeMailer mailer;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    /** Compared against when there is no usable account, so response time does not reveal which usernames exist. */
    private final String decoyHash;

    public AuthServiceImpl(MemberRepository memberRepository,
                           PasswordEncoder passwordEncoder, JwtService jwtService, LoginThrottle throttle,
                           MemberService memberService,
                           ResetCodeMailer mailer, Clock clock) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.throttle = throttle;
        this.memberService = memberService;
        this.mailer = mailer;
        this.clock = clock;
        this.decoyHash = passwordEncoder.encode("decoy-password-never-matches");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        throttle.checkAllowed(username);

        Member account = findAccount(username);

        String hash = account != null && account.getPasswordHash() != null ? account.getPasswordHash() : decoyHash;
        boolean passwordMatches = matches(request.password(), hash);

        if (account != null && account.getPasswordHash() != null && passwordMatches && canLogIn(account)) {
            throttle.recordSuccess(username);
            return accountLogin(account);
        }
        throttle.recordFailure(username);
        throw new InvalidCredentialsException();
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new BusinessException("Mật khẩu tối đa 72 byte (tiếng Việt có dấu tính nhiều byte hơn).");
        }
        // MemberRequest cleans the phone number and the other fields the same way the librarian's form does.
        MemberResponse created = memberService.register(
                new MemberRequest(request.fullName(), request.email(), null), request.password());
        Member member = memberRepository.findByLibraryCardNo(created.libraryCardNo()).orElseThrow();
        return accountLogin(member);
    }

    @Override
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = normalize(request.email());
        Member member = memberRepository.findByEmailIgnoreCase(email).orElse(null);
        if (member == null || member.getStatus() == MemberStatus.LOCKED) {
            return; // same answer as for a real account
        }
        Instant now = clock.instant();
        if (member.getResetCodeSentAt() != null && now.isBefore(member.getResetCodeSentAt().plus(RESEND_COOLDOWN))) {
            return;
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        member.setResetCodeHash(passwordEncoder.encode(code));
        member.setResetCodeExpiresAt(now.plus(Duration.ofMinutes(CODE_VALID_MINUTES)));
        member.setResetCodeAttempts(0);
        member.setResetCodeSentAt(now);
        memberRepository.save(member);
        try {
            mailer.send(member.getEmail(), member.getFullName(), code, CODE_VALID_MINUTES);
        } catch (RuntimeException e) {
            // The reader only sees the generic answer; the cause is for whoever runs the server.
            log.error("Could not send the reset code to {}: {}", member.getEmail(), e.getMessage());
        }
    }

    @Override
    public void verifyResetCode(VerifyResetCodeRequest request) {
        checkCode(normalize(request.email()), request.code());
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new BusinessException("Mật khẩu mới tối đa 72 byte (tiếng Việt có dấu tính nhiều byte hơn).");
        }
        String email = normalize(request.email());
        // The code is checked again here, so the new-password step cannot be reached without it.
        Member member = checkCode(email, request.code());

        member.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        member.setResetCodeHash(null); // a code works once
        member.setResetCodeExpiresAt(null);
        member.setResetCodeAttempts(0);
        member.setResetCodeSentAt(null);
        memberRepository.save(member);
        throttle.recordSuccess("reset:" + email);
    }

    /**
     * Returns the member when the code is the one e-mailed to them and still valid. A wrong guess counts: five of
     * them cancel the code. A right guess changes nothing, so the same code can then be used to set the password.
     */
    private Member checkCode(String email, String code) {
        String throttleKey = "reset:" + email;
        throttle.checkAllowed(throttleKey);

        Member member = memberRepository.findByEmailIgnoreCase(email).orElse(null);
        Instant now = clock.instant();
        boolean usable = member != null && member.getResetCodeHash() != null && member.getResetCodeExpiresAt() != null
                && now.isBefore(member.getResetCodeExpiresAt()) && member.getResetCodeAttempts() < MAX_CODE_ATTEMPTS;
        boolean codeMatches = passwordEncoder.matches(code, usable ? member.getResetCodeHash() : decoyHash);

        if (!usable || !codeMatches) {
            throttle.recordFailure(throttleKey);
            if (usable) {
                member.setResetCodeAttempts(member.getResetCodeAttempts() + 1);
                if (member.getResetCodeAttempts() >= MAX_CODE_ATTEMPTS) {
                    member.setResetCodeHash(null); // the code cannot be guessed at any further
                    member.setResetCodeExpiresAt(null);
                }
                memberRepository.save(member);
            }
            throw new BusinessException(BAD_CODE);
        }
        return member;
    }

    @Override
    public void changePassword(String role, String username, ChangePasswordRequest request) {
        throttle.checkAllowed(username);

        Member account = memberRepository.findByLibraryCardNo(username)
                .filter(m -> m.getRole().name().equals(role)).orElse(null);
        String currentHash = account != null && account.getPasswordHash() != null ? account.getPasswordHash() : decoyHash;

        if (account == null || !matches(request.currentPassword(), currentHash)) {
            throttle.recordFailure(username);
            throw new InvalidCredentialsException();
        }
        throttle.recordSuccess(username);

        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new BusinessException("Mật khẩu mới tối đa 72 byte (tiếng Việt có dấu tính nhiều byte hơn).");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        memberRepository.save(account);
    }

    /** A reader types the card number or the e-mail they registered with; a librarian types the staff code. */
    private Member findAccount(String username) {
        if (username.contains("@")) {
            return memberRepository.findByEmailIgnoreCase(username).orElse(null);
        }
        return memberRepository.findByLibraryCardNo(username).orElse(null);
    }

    /** A librarian must be ACTIVE; a suspended reader may still sign in to see their loans, only a locked card is shut out. */
    private static boolean canLogIn(Member account) {
        return account.getRole() == MemberRole.LIBRARIAN
                ? account.getStatus() == MemberStatus.ACTIVE
                : account.getStatus() != MemberStatus.LOCKED;
    }

    private LoginResponse accountLogin(Member account) {
        IssuedToken issued = jwtService.issue(account);
        return LoginResponse.bearer(issued.token(), issued.expiresAt(), account.getRole().name(),
                account.getLibraryCardNo(), account.getFullName());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matches(String rawPassword, String hash) {
        return passwordEncoder.matches(rawPassword, hash)
                && rawPassword.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}
