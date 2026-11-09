package fu.dlms.member.service;

import fu.dlms.member.dto.ChangePasswordRequest;
import fu.dlms.member.dto.LoginRequest;
import fu.dlms.member.dto.LoginResponse;

public interface AuthService {

    /**
     * Logs in a librarian (staff code) or a reader (library card number).
     *
     * @throws fu.dlms.member.exception.InvalidCredentialsException for any failed login
     * @throws fu.dlms.member.exception.TooManyLoginAttemptsException after too many wrong passwords
     */
    LoginResponse login(LoginRequest request);

    /**
     * Changes the password of the caller. {@code role} and {@code username} come from the verified token (the
     * gateway forwards them), never from the request body, so nobody can change someone else's password.
     */
    void changePassword(String role, String username, ChangePasswordRequest request);
}
