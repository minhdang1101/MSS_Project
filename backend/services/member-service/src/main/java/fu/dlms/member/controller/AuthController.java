package fu.dlms.member.controller;

import fu.dlms.member.dto.ChangePasswordRequest;
import fu.dlms.member.dto.ForgotPasswordRequest;
import fu.dlms.member.dto.RegisterRequest;
import fu.dlms.member.dto.ResetPasswordRequest;
import fu.dlms.member.dto.VerifyResetCodeRequest;
import fu.dlms.member.dto.LoginRequest;
import fu.dlms.member.dto.LoginResponse;
import fu.dlms.member.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Librarians sign in with a staff code, readers with a library card number. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** A reader signs up on their own and is logged in at once. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** Always 204, whether or not the e-mail belongs to an account. */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
    }

    /** Always the same cheap check: 204 when the code is right, 409 when it is not or has expired. */
    @PostMapping("/verify-reset-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        authService.verifyResetCode(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }


    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestHeader("X-User-Role") String role, @RequestHeader("X-Username") String username,
                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(role, username, request);
    }
}
