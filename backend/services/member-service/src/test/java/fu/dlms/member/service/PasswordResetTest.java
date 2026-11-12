package fu.dlms.member.service;

import fu.dlms.member.dto.req.ForgotPasswordRequest;
import fu.dlms.member.dto.req.LoginRequest;
import fu.dlms.member.dto.res.MemberResponse;
import fu.dlms.member.dto.req.RegisterRequest;
import fu.dlms.member.dto.req.ResetPasswordRequest;
import fu.dlms.member.dto.req.VerifyResetCodeRequest;
import fu.dlms.member.entity.Member;
import fu.dlms.member.entity.MemberStatus;
import fu.dlms.member.exception.BusinessException;
import fu.dlms.member.exception.InvalidCredentialsException;
import fu.dlms.member.repository.MemberRepository;
import fu.dlms.member.security.IssuedToken;
import fu.dlms.member.security.JwtService;
import fu.dlms.member.security.LoginThrottle;
import fu.dlms.member.security.ResetCodeMailer;
import fu.dlms.member.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final MemberRepository members = mock(MemberRepository.class);
    private final MemberService memberService = mock(MemberService.class);
    private final ResetCodeMailer mailer = mock(ResetCodeMailer.class);
    private final JwtService jwt = mock(JwtService.class);
    private Instant now = Instant.parse("2026-11-09T10:00:00Z");
    private AuthServiceImpl service;
    private Member member;

    @BeforeEach
    void setUp() {
        Clock clock = new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now;
            }
        };
        service = new AuthServiceImpl(members, encoder, jwt, new LoginThrottle(clock),
                memberService, mailer, clock);

        member = new Member();
        member.setId(7L);
        member.setLibraryCardNo("TV-0007");
        member.setFullName("Lê Văn An");
        member.setEmail("an@example.com");
        member.setStatus(MemberStatus.ACTIVE);
        member.setPasswordHash(encoder.encode("old-password"));
        when(members.findByEmailIgnoreCase("an@example.com")).thenReturn(Optional.of(member));

        when(members.save(any(Member.class))).thenAnswer(i -> i.getArgument(0));
    }

    private String requestCode() {
        service.requestPasswordReset(new ForgotPasswordRequest("An@Example.com "));
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq("an@example.com"), eq("Lê Văn An"), code.capture(), eq(10));
        return code.getValue();
    }

    @Test
    void rightCodeSetsTheNewPasswordOnce() {
        String code = requestCode();

        service.resetPassword(new ResetPasswordRequest("an@example.com", code, "brand-new-pass"));

        assertThat(encoder.matches("brand-new-pass", member.getPasswordHash())).isTrue();
        assertThat(member.getResetCodeHash()).isNull();
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("an@example.com", code, "another-pass1")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rightCodeIsConfirmedWithoutBeingUsedUp() {
        String code = requestCode();

        service.verifyResetCode(new VerifyResetCodeRequest("an@example.com", code));
        service.verifyResetCode(new VerifyResetCodeRequest("an@example.com", code)); // can be checked again

        assertThat(member.getResetCodeHash()).isNotNull();
        service.resetPassword(new ResetPasswordRequest("an@example.com", code, "brand-new-pass")); // and still works after
        assertThat(encoder.matches("brand-new-pass", member.getPasswordHash())).isTrue();
    }

    @Test
    void wrongCodeIsRejectedAtTheVerifyStepAndCounts() {
        String code = requestCode();
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.verifyResetCode(new VerifyResetCodeRequest("an@example.com", wrong)))
                    .isInstanceOf(BusinessException.class);
        }
        assertThat(member.getResetCodeHash()).isNull();
        // the real code is dead too once five guesses were spent (the e-mail is also locked for a few minutes)
        assertThatThrownBy(() -> service.verifyResetCode(new VerifyResetCodeRequest("an@example.com", code)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void unknownEmailFailsTheVerifyStepLikeAWrongCode() {
        assertThatThrownBy(() -> service.verifyResetCode(new VerifyResetCodeRequest("nobody@example.com", "123456")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void codeIsSixDigits() {
        assertThat(requestCode()).matches("[0-9]{6}");
    }

    @Test
    void unknownEmailGetsTheSameSilentAnswerAndNoMail() {
        service.requestPasswordReset(new ForgotPasswordRequest("nobody@example.com"));
        verify(mailer, never()).send(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
        assertThat(member.getResetCodeHash()).isNull();
    }

    @Test
    void lockedCardGetsNoCode() {
        member.setStatus(MemberStatus.LOCKED);
        service.requestPasswordReset(new ForgotPasswordRequest("an@example.com"));
        verify(mailer, never()).send(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void askingAgainWithinAMinuteSendsNothingNew() {
        requestCode();
        service.requestPasswordReset(new ForgotPasswordRequest("an@example.com"));
        verify(mailer).send(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt()); // still only the first mail

        now = now.plus(Duration.ofSeconds(61));
        service.requestPasswordReset(new ForgotPasswordRequest("an@example.com"));
        verify(mailer, org.mockito.Mockito.times(2)).send(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void wrongCodeIsRefusedAndPasswordKept() {
        requestCode();
        String oldHash = member.getPasswordHash();

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("an@example.com", "000000", "brand-new-pass")))
                .isInstanceOf(BusinessException.class);

        assertThat(member.getPasswordHash()).isEqualTo(oldHash);
    }

    @Test
    void fiveWrongGuessesCancelTheCode() {
        String code = requestCode();
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("an@example.com", wrong, "brand-new-pass")))
                    .isInstanceOf(BusinessException.class);
        }
        assertThat(member.getResetCodeHash()).isNull();
    }

    @Test
    void expiredCodeIsRefused() {
        String code = requestCode();
        now = now.plus(Duration.ofMinutes(11));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("an@example.com", code, "brand-new-pass")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void registerIssuesACardAndLogsTheReaderIn() {
        MemberResponse created = mock(MemberResponse.class);
        when(created.libraryCardNo()).thenReturn("TV-0007");
        when(memberService.register(any(), eq("my-secret-pass"))).thenReturn(created);
        when(members.findByLibraryCardNo("TV-0007")).thenReturn(Optional.of(member));
        when(jwt.issue(any(Member.class))).thenReturn(new IssuedToken("member-jwt", now.plusSeconds(3600)));

        var response = service.register(new RegisterRequest("Lê Văn An", "an@example.com", "0912 345 678", "my-secret-pass"));

        assertThat(response.token()).isEqualTo("member-jwt");
        assertThat(response.role()).isEqualTo("MEMBER");
        assertThat(response.username()).isEqualTo("TV-0007");
    }

    @Test
    void readerCanLogInWithTheEmail() {
        when(jwt.issue(any(Member.class))).thenReturn(new IssuedToken("member-jwt", now.plusSeconds(3600)));

        var response = service.login(new LoginRequest("an@example.com", "old-password"));

        assertThat(response.username()).isEqualTo("TV-0007");
        assertThatThrownBy(() -> service.login(new LoginRequest("an@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
