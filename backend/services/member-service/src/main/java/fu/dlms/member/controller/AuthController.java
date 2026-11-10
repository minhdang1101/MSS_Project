package fu.dlms.member.controller;

import fu.dlms.member.dto.MemberResponse;
import fu.dlms.member.dto.RegisterRequest;
import fu.dlms.member.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-in, forgot-password and password change are Keycloak's pages. Only signing up stays here, because it also
 * issues the library card number and creates the reader's profile.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** A reader signs up on their own: 201 with the new card number; they sign in afterwards on Keycloak's page. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
}
