package fu.dlms.member.service.impl;

import fu.dlms.member.dto.LoginRequest;
import fu.dlms.member.dto.LoginResponse;
import fu.dlms.member.entity.Librarian;
import fu.dlms.member.exception.InvalidCredentialsException;
import fu.dlms.member.repository.LibrarianRepository;
import fu.dlms.member.security.IssuedToken;
import fu.dlms.member.security.JwtService;
import fu.dlms.member.security.LoginThrottle;
import fu.dlms.member.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Deliberately not @Transactional: the repository call opens its own short transaction, so a database connection
 * is not held while the slow BCrypt comparison runs.
 */
@Service
public class AuthServiceImpl implements AuthService {

    /** BCrypt only looks at the first 72 bytes, so a longer password would silently match its own prefix. */
    private static final int BCRYPT_MAX_BYTES = 72;

    private final LibrarianRepository librarianRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginThrottle throttle;
    /** Compared against when the staff code is unknown, so response time does not reveal which codes exist. */
    private final String decoyHash;

    public AuthServiceImpl(LibrarianRepository librarianRepository, PasswordEncoder passwordEncoder,
                           JwtService jwtService, LoginThrottle throttle) {
        this.librarianRepository = librarianRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.throttle = throttle;
        this.decoyHash = passwordEncoder.encode("decoy-password-never-matches");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String staffCode = request.staffCode().trim();
        throttle.checkAllowed(staffCode);

        Librarian librarian = librarianRepository.findByStaffCode(staffCode).orElse(null);
        String hash = librarian != null ? librarian.getPasswordHash() : decoyHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash)
                && request.password().getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;

        if (librarian == null || !passwordMatches || !librarian.isActive()) {
            throttle.recordFailure(staffCode);
            throw new InvalidCredentialsException();
        }
        throttle.recordSuccess(staffCode);
        IssuedToken issued = jwtService.issue(librarian);
        return LoginResponse.bearer(issued.token(), issued.expiresAt(), librarian.getStaffCode(), librarian.getFullName());
    }
}
