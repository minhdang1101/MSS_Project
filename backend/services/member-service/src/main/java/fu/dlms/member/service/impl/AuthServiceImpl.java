package fu.dlms.member.service.impl;

import fu.dlms.member.dto.ChangePasswordRequest;
import fu.dlms.member.dto.LoginRequest;
import fu.dlms.member.dto.LoginResponse;
import fu.dlms.member.entity.Librarian;
import fu.dlms.member.entity.Member;
import fu.dlms.member.entity.MemberStatus;
import fu.dlms.member.exception.BusinessException;
import fu.dlms.member.exception.InvalidCredentialsException;
import fu.dlms.member.repository.LibrarianRepository;
import fu.dlms.member.repository.MemberRepository;
import fu.dlms.member.security.IssuedToken;
import fu.dlms.member.security.JwtService;
import fu.dlms.member.security.LoginThrottle;
import fu.dlms.member.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Deliberately not @Transactional: each repository call opens its own short transaction, so a database connection
 * is not held while the slow BCrypt comparison runs.
 */
@Service
public class AuthServiceImpl implements AuthService {

    static final String LIBRARIAN = "LIBRARIAN";
    static final String MEMBER = "MEMBER";
    /** BCrypt only looks at the first 72 bytes, so a longer password would silently match its own prefix. */
    private static final int BCRYPT_MAX_BYTES = 72;

    private final LibrarianRepository librarianRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginThrottle throttle;
    /** Compared against when there is no usable account, so response time does not reveal which usernames exist. */
    private final String decoyHash;

    public AuthServiceImpl(LibrarianRepository librarianRepository, MemberRepository memberRepository,
                           PasswordEncoder passwordEncoder, JwtService jwtService, LoginThrottle throttle) {
        this.librarianRepository = librarianRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.throttle = throttle;
        this.decoyHash = passwordEncoder.encode("decoy-password-never-matches");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        throttle.checkAllowed(username);

        Librarian librarian = librarianRepository.findByStaffCode(username).orElse(null);
        Member member = librarian == null ? memberRepository.findByLibraryCardNo(username).orElse(null) : null;

        String hash = decoyHash;
        if (librarian != null) {
            hash = librarian.getPasswordHash();
        } else if (member != null && member.getPasswordHash() != null) {
            hash = member.getPasswordHash();
        }
        boolean passwordMatches = matches(request.password(), hash);

        if (librarian != null && passwordMatches && librarian.isActive()) {
            throttle.recordSuccess(username);
            IssuedToken issued = jwtService.issue(librarian);
            return LoginResponse.bearer(issued.token(), issued.expiresAt(), LIBRARIAN, librarian.getStaffCode(), librarian.getFullName());
        }
        // A suspended reader may still log in to see their loans and fines; only a locked card is shut out.
        if (member != null && member.getPasswordHash() != null && passwordMatches && member.getStatus() != MemberStatus.LOCKED) {
            throttle.recordSuccess(username);
            IssuedToken issued = jwtService.issue(member);
            return LoginResponse.bearer(issued.token(), issued.expiresAt(), MEMBER, member.getLibraryCardNo(), member.getFullName());
        }
        throttle.recordFailure(username);
        throw new InvalidCredentialsException();
    }

    @Override
    public void changePassword(String role, String username, ChangePasswordRequest request) {
        throttle.checkAllowed(username);

        Librarian librarian = LIBRARIAN.equals(role) ? librarianRepository.findByStaffCode(username).orElse(null) : null;
        Member member = MEMBER.equals(role) ? memberRepository.findByLibraryCardNo(username).orElse(null) : null;
        String currentHash = librarian != null ? librarian.getPasswordHash()
                : member != null && member.getPasswordHash() != null ? member.getPasswordHash() : decoyHash;

        if ((librarian == null && member == null) || !matches(request.currentPassword(), currentHash)) {
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
        String newHash = passwordEncoder.encode(request.newPassword());
        if (librarian != null) {
            librarian.setPasswordHash(newHash);
            librarianRepository.save(librarian);
        } else {
            member.setPasswordHash(newHash);
            memberRepository.save(member);
        }
    }

    private boolean matches(String rawPassword, String hash) {
        return passwordEncoder.matches(rawPassword, hash)
                && rawPassword.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}
