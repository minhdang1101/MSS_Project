package fu.dlms.member.service;

import fu.dlms.member.dto.req.ChangePasswordRequest;
import fu.dlms.member.dto.req.ForgotPasswordRequest;
import fu.dlms.member.dto.req.LoginRequest;
import fu.dlms.member.dto.res.LoginResponse;
import fu.dlms.member.dto.req.RegisterRequest;
import fu.dlms.member.dto.req.ResetPasswordRequest;
import fu.dlms.member.dto.req.VerifyResetCodeRequest;

public interface AuthService {

    /**
     * Logs in a librarian (staff code) or a reader (library card number or e-mail).
     *
     * @throws fu.dlms.member.exception.InvalidCredentialsException for any failed login
     * @throws fu.dlms.member.exception.TooManyLoginAttemptsException after too many wrong passwords
     */
    LoginResponse login(LoginRequest request);

    /** A reader signs up and is logged in straight away. 409 when the e-mail or phone is already used. */
    LoginResponse register(RegisterRequest request);

    /**
     * Sends a 6-digit code to the member's e-mail. The answer never says whether the e-mail exists, so the
     * form cannot be used to find out who has an account.
     */
    void requestPasswordReset(ForgotPasswordRequest request);

    /** Step 2: says whether the code is right (409 if not) without using it up, so the new-password step can follow. */
    void verifyResetCode(VerifyResetCodeRequest request);

    /** Sets a new password when the code is right and not expired. Five wrong codes cancel the code. */
    void resetPassword(ResetPasswordRequest request);

    /**
     * Changes the password of the caller. {@code role} and {@code username} come from the verified token (the
     * gateway forwards them), never from the request body, so nobody can change someone else's password.
     */
    void changePassword(String role, String username, ChangePasswordRequest request);
}
