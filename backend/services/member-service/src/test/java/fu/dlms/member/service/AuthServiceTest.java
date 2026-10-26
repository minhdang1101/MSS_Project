package fu.dlms.member.service;

import fu.dlms.member.dto.LoginRequest;
import fu.dlms.member.dto.LoginResponse;
import fu.dlms.member.entity.Librarian;
import fu.dlms.member.exception.InvalidCredentialsException;
import fu.dlms.member.exception.TooManyLoginAttemptsException;
import fu.dlms.member.repository.LibrarianRepository;
import fu.dlms.member.security.IssuedToken;
import fu.dlms.member.security.JwtService;
import fu.dlms.member.security.LoginThrottle;
import fu.dlms.member.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final LibrarianRepository repository = mock(LibrarianRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private AuthServiceImpl authService;
    private Librarian librarian;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(repository, encoder, jwtService, new LoginThrottle(Clock.systemUTC()));
        librarian = new Librarian();
        librarian.setStaffCode("TT-0001");
        librarian.setFullName("Nguyễn Thu Trang");
        librarian.setPasswordHash(encoder.encode("correct-password"));
        librarian.setActive(true);
        when(repository.findByStaffCode("TT-0001")).thenReturn(Optional.of(librarian));
        when(jwtService.issue(any())).thenReturn(new IssuedToken("jwt-value", Instant.parse("2026-10-26T16:00:00Z")));
    }

    @Test
    void correctCredentialsReturnTokenAndName() {
        LoginResponse response = authService.login(new LoginRequest("TT-0001", "correct-password"));

        assertThat(response.token()).isEqualTo("jwt-value");
        assertThat(response.fullName()).isEqualTo("Nguyễn Thu Trang");
        assertThat(response.expiresAt()).isEqualTo(Instant.parse("2026-10-26T16:00:00Z"));
    }

    @Test
    void staffCodeIsTrimmedBeforeLookup() {
        assertThat(authService.login(new LoginRequest("  TT-0001 ", "correct-password")).token()).isEqualTo("jwt-value");
    }

    @Test
    void wrongPasswordIsRejectedWithoutIssuingToken() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("TT-0001", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).issue(any());
    }

    @Test
    void unknownStaffCodeIsRejectedWithSameError() {
        when(repository.findByStaffCode("TT-9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("TT-9999", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Sai mã thủ thư hoặc mật khẩu");
    }

    @Test
    void deactivatedLibrarianCannotLogInEvenWithCorrectPassword() {
        librarian.setActive(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("TT-0001", "correct-password")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).issue(any());
    }

    @Test
    void passwordLongerThan72BytesIsRejectedBecauseBcryptWouldSilentlyTruncateIt() {
        String vietnamese = "ế".repeat(30); // 30 characters but 90 bytes

        assertThatThrownBy(() -> authService.login(new LoginRequest("TT-0001", vietnamese)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void fiveWrongPasswordsLockTheAccountEvenAgainstTheCorrectPassword() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("TT-0001", "wrong")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(() -> authService.login(new LoginRequest("TT-0001", "correct-password")))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        verify(jwtService, never()).issue(any());
    }

    @Test
    void aSuccessfulLoginResetsTheFailureCount() {
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("TT-0001", "wrong")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
        authService.login(new LoginRequest("TT-0001", "correct-password"));

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("TT-0001", "wrong")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
        assertThat(authService.login(new LoginRequest("TT-0001", "correct-password")).token()).isEqualTo("jwt-value");
    }
}
