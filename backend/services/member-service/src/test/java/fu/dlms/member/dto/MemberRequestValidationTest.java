package fu.dlms.member.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MemberRequestValidationTest {

    private static final String PHONE = "0900000001";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /** A well-formed address of exactly {@code length} characters (local part 64, then domain). */
    private static String email(int length) {
        return "a".repeat(64) + "@" + "b".repeat(length - 64 - 1 - 4) + ".com";
    }

    private static String chars(int n) {
        return "a".repeat(n);
    }

    private static <T> Set<String> invalidFields(Set<ConstraintViolation<T>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void acceptsValuesAtTheMaximumLength() {
        assertThat(validator.validate(new MemberRequest(chars(100), email(100), "0".repeat(15)))).isEmpty();
    }

    @Test
    void rejectsEveryFieldWhenNull() {
        assertThat(invalidFields(validator.validate(new MemberRequest(null, null, null))))
                .containsExactlyInAnyOrder("fullName", "email", "phone");
    }

    @Test
    void rejectsBlankValuesIncludingWhitespaceOnlyWithOneMessageEach() {
        Set<ConstraintViolation<MemberRequest>> violations = validator.validate(new MemberRequest("   ", " ", "\t"));

        assertThat(invalidFields(violations)).containsExactlyInAnyOrder("fullName", "email", "phone");
        assertThat(violations).hasSize(3);
    }

    @Test
    void rejectsNameAndEmailLongerThan100() {
        assertThat(invalidFields(validator.validate(new MemberRequest(chars(101), email(101), PHONE))))
                .containsExactlyInAnyOrder("fullName", "email");
    }

    @Test
    void rejectsMalformedEmail() {
        assertThat(invalidFields(validator.validate(new MemberRequest("Tên", "not-an-email", PHONE))))
                .containsExactly("email");
    }

    // ---------- normalisation: makes "same person, different typing" collapse to one value ----------

    @Test
    void trimsNameAndEmail() {
        MemberRequest request = new MemberRequest("  Nguyễn Văn A  ", "  a@example.com ", PHONE);

        assertThat(request.fullName()).isEqualTo("Nguyễn Văn A");
        assertThat(request.email()).isEqualTo("a@example.com");
    }

    @Test
    void stripsSpacesDotsDashesAndBracketsFromThePhoneSoDifferentTypingsAreTheSameNumber() {
        assertThat(new MemberRequest("A", "a@example.com", " 0912 345 678 ").phone()).isEqualTo("0912345678");
        assertThat(new MemberRequest("A", "a@example.com", "0912.345.678").phone()).isEqualTo("0912345678");
        assertThat(new MemberRequest("A", "a@example.com", "(091) 234-5678").phone()).isEqualTo("0912345678");
    }

    @Test
    void phoneMustBe8To15DigitsWithAnOptionalLeadingPlus() {
        assertThat(validator.validate(new MemberRequest("A", "a@example.com", "+84912345678"))).isEmpty();
        assertThat(validator.validate(new MemberRequest("A", "a@example.com", "12345678"))).isEmpty();
        for (String bad : new String[]{"abc", "1234567", "0".repeat(16), "09123x5678", "0912+345678"}) {
            assertThat(invalidFields(validator.validate(new MemberRequest("A", "a@example.com", bad))))
                    .as("phone %s", bad).containsExactly("phone");
        }
    }

    @Test
    void violationMessagesAreInVietnameseSoTheFormCanShowThemAsIs() {
        String blank = validator.validate(new MemberRequest("", "a@example.com", PHONE)).iterator().next().getMessage();
        String tooLong = validator.validate(new MemberRequest(chars(101), "a@example.com", PHONE)).iterator().next().getMessage();
        String badPhone = validator.validate(new MemberRequest("A", "a@example.com", "abc")).iterator().next().getMessage();

        assertThat(blank).isEqualTo("Không được để trống");
        assertThat(tooLong).isEqualTo("Tối đa 100 ký tự");
        assertThat(badPhone).isEqualTo("Số điện thoại không hợp lệ (8-15 chữ số)");
    }
}
