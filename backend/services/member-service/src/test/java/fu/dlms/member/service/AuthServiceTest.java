package fu.dlms.member.service;

import fu.dlms.member.dto.ChangePasswordRequest;
import fu.dlms.member.dto.LoginRequest;
import fu.dlms.member.dto.LoginResponse;
import fu.dlms.member.entity.Member;
import fu.dlms.member.entity.MemberRole;
import fu.dlms.member.entity.MemberStatus;
import fu.dlms.member.exception.BusinessException;
import fu.dlms.member.exception.InvalidCredentialsException;
import fu.dlms.member.exception.TooManyLoginAttemptsException;
import fu.dlms.member.repository.MemberRepository;
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

    private static final Instant EXPIRY = Instant.parse("2026-10-26T16:00:00Z");

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final MemberRepository members = mock(MemberRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private AuthServiceImpl authService;
    private Member librarian;
    private Member member;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(members, encoder, jwtService, new LoginThrottle(Clock.systemUTC()),
                mock(MemberService.class),
                mock(fu.dlms.member.security.ResetCodeMailer.class), Clock.systemUTC());

        librarian = new Member();
        librarian.setRole(MemberRole.LIBRARIAN);
        librarian.setLibraryCardNo("TT-0001");
        librarian.setFullName("Nguyễn Thu Trang");
        librarian.setPasswordHash(encoder.encode("correct-password"));
        librarian.setStatus(MemberStatus.ACTIVE);
        when(members.findByLibraryCardNo("TT-0001")).thenReturn(Optional.of(librarian));

        member = new Member();
        member.setId(5L);
        member.setLibraryCardNo("TV-0412");
        member.setFullName("Phạm Minh Khôi");
        member.setPasswordHash(encoder.encode("reader-password"));
        member.setStatus(MemberStatus.ACTIVE);
        when(members.findByLibraryCardNo("TV-0412")).thenReturn(Optional.of(member));
        when(jwtService.issue(any(Member.class))).thenAnswer(i ->
                new IssuedToken(((Member) i.getArgument(0)).getRole() == MemberRole.LIBRARIAN ? "librarian-jwt" : "member-jwt", EXPIRY));
    }

    // ---------- librarians ----------

    @Test
    void correctCredentialsReturnTokenNameAndTheLibrarianRole() {
        LoginResponse response = authService.login(new LoginRequest("TT-0001", "correct-password"));

        assertThat(response.token()).isEqualTo("librarian-jwt");
        assertThat(response.role()).isEqualTo("LIBRARIAN");
        assertThat(response.username()).isEqualTo("TT-0001");
        assertThat(response.fullName()).isEqualTo("Nguyễn Thu Trang");
        assertThat(response.expiresAt()).isEqualTo(EXPIRY);
    }

    @Test
    void usernameIsTrimmedBeforeLookup() {
        assertThat(authService.login(new LoginRequest("  TT-0001 ", "correct-password")).token()).isEqualTo("librarian-jwt");
    }

    @Test
    void wrongPasswordIsRejectedWithoutIssuingAToken() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("TT-0001", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).issue(any(Member.class));
    }

    @Test
    void anUnknownUsernameGetsTheSameErrorAsAWrongPassword() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("NOBODY", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Sai tài khoản hoặc mật khẩu");
    }

    @Test
    void deactivatedLibrarianCannotLogInEvenWithCorrectPassword() {
        librarian.setStatus(MemberStatus.SUSPENDED);

        assertThatThrownBy(() -> authService.login(new LoginRequest("TT-0001", "correct-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ---------- readers ----------

    @Test
    void aReaderLogsInWithTheLibraryCardNumberAndGetsTheMemberRole() {
        LoginResponse response = authService.login(new LoginRequest("TV-0412", "reader-password"));

        assertThat(response.token()).isEqualTo("member-jwt");
        assertThat(response.role()).isEqualTo("MEMBER");
        assertThat(response.username()).isEqualTo("TV-0412");
        assertThat(response.fullName()).isEqualTo("Phạm Minh Khôi");
    }

    @Test
    void aSuspendedReaderCanStillLogInToSeeTheirLoansAndFines() {
        member.setStatus(MemberStatus.SUSPENDED);

        assertThat(authService.login(new LoginRequest("TV-0412", "reader-password")).role()).isEqualTo("MEMBER");
    }

    @Test
    void aLockedCardCannotLogIn() {
        member.setStatus(MemberStatus.LOCKED);

        assertThatThrownBy(() -> authService.login(new LoginRequest("TV-0412", "reader-password")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).issue(any(Member.class));
    }

    @Test
    void aReaderWhoWasNeverGivenAPasswordCannotLogIn() {
        member.setPasswordHash(null);

        assertThatThrownBy(() -> authService.login(new LoginRequest("TV-0412", "anything")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void aWrongReaderPasswordIsRejected() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("TV-0412", "nope")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).issue(any(Member.class));
    }

    // ---------- shared rules ----------

    @Test
    void passwordLongerThan72BytesIsRejectedBecauseBcryptWouldSilentlyTruncateIt() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("TT-0001", "ế".repeat(30))))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void fiveWrongPasswordsLockTheAccountEvenAgainstTheCorrectPassword() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("TV-0412", "wrong")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(() -> authService.login(new LoginRequest("TV-0412", "reader-password")))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test
    void aSuccessfulLoginResetsTheFailureCount() {
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("TV-0412", "wrong")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
        authService.login(new LoginRequest("TV-0412", "reader-password"));

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("TV-0412", "wrong")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
        assertThat(authService.login(new LoginRequest("TV-0412", "reader-password")).token()).isEqualTo("member-jwt");
    }

    // ---------- changing a password ----------

    @Test
    void aReaderChangesTheirPasswordAndTheNewOneWorks() {
        authService.changePassword("MEMBER", "TV-0412", new ChangePasswordRequest("reader-password", "brand-new-pass"));

        assertThat(encoder.matches("brand-new-pass", member.getPasswordHash())).isTrue();
        assertThat(encoder.matches("reader-password", member.getPasswordHash())).isFalse();
        verify(members).save(member);
    }

    @Test
    void aLibrarianChangesTheirPasswordToo() {
        authService.changePassword("LIBRARIAN", "TT-0001", new ChangePasswordRequest("correct-password", "another-pass-1"));

        assertThat(encoder.matches("another-pass-1", librarian.getPasswordHash())).isTrue();
        verify(members).save(librarian);
    }

    @Test
    void theCurrentPasswordMustBeRight() {
        assertThatThrownBy(() -> authService.changePassword("MEMBER", "TV-0412",
                new ChangePasswordRequest("not-my-password", "brand-new-pass")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(members, never()).save(any());
    }

    @Test
    void theNewPasswordMustDifferFromTheCurrentOne() {
        assertThatThrownBy(() -> authService.changePassword("MEMBER", "TV-0412",
                new ChangePasswordRequest("reader-password", "reader-password")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("khác");
    }

    @Test
    void aNewPasswordOverSeventyTwoBytesIsRefused() {
        assertThatThrownBy(() -> authService.changePassword("MEMBER", "TV-0412",
                new ChangePasswordRequest("reader-password", "ế".repeat(30))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("72");
    }

    @Test
    void anUnknownRoleOrUserCannotChangeAPassword() {
        assertThatThrownBy(() -> authService.changePassword("ADMIN", "TV-0412",
                new ChangePasswordRequest("reader-password", "brand-new-pass")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authService.changePassword("MEMBER", "TV-9999",
                new ChangePasswordRequest("x", "brand-new-pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void wrongCurrentPasswordsAreCountedAgainstTheLoginLimitToo() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.changePassword("MEMBER", "TV-0412",
                    new ChangePasswordRequest("wrong", "brand-new-pass")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(() -> authService.changePassword("MEMBER", "TV-0412",
                new ChangePasswordRequest("reader-password", "brand-new-pass")))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }
}
