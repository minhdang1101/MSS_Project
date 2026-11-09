package fu.dlms.catalog.dto;

import fu.dlms.catalog.entity.BookFormat;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private BookRequest request(String isbn, Integer year) {
        return new BookRequest(isbn, "Tên", "Tác giả", null, year, BookFormat.PHYSICAL, 1);
    }

    @Test
    void thirteenDigitsIsAccepted() {
        assertThat(validator.validate(request("9786041087652", 2020))).isEmpty();
    }

    @Test
    void anyOtherIsbnIsRejected() {
        for (String bad : new String[] {"978604108765", "97860410876523", "978-604-1-08765-2", "97860410876AB", " "}) {
            assertThat(validator.validate(request(bad, 2020))).as(bad).isNotEmpty();
        }
    }

    @Test
    void yearIsOptionalButNotAbsurd() {
        assertThat(validator.validate(request("9786041087652", null))).isEmpty();
        assertThat(validator.validate(request("9786041087652", 15))).isNotEmpty();
    }
}
