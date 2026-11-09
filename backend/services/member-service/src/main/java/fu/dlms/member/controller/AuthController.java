package fu.dlms.member.controller;

import fu.dlms.member.dto.ChangePasswordRequest;
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

    /**
     * Changes the caller's own password. Who the caller is comes from {@code X-User-Role} and {@code X-Username},
     * which the API gateway sets from the verified token and overwrites on every request, so they cannot be forged
     * through the gateway. (Like every service, this one must not be reachable except through the gateway.)
     */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestHeader("X-User-Role") String role, @RequestHeader("X-Username") String username,
                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(role, username, request);
    }
}
