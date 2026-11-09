package fu.dlms.member.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordGeneratorTest {

    private final PasswordGenerator generator = new PasswordGenerator();

    @Test
    void passwordsAreTwelveCharactersLongAndSafeToReadAloud() {
        for (int i = 0; i < 200; i++) {
            String password = generator.generate();

            assertThat(password).hasSize(12);
            // no look-alike characters (0/O, 1/l/I) that a reader could mistype from a printout
            assertThat(password).matches("[A-HJ-NP-Za-km-z2-9]{12}");
        }
    }

    @Test
    void everyPasswordMixesUpperLowerAndDigits() {
        for (int i = 0; i < 200; i++) {
            String password = generator.generate();

            assertThat(password).matches(".*[A-Z].*").matches(".*[a-z].*").matches(".*[0-9].*");
        }
    }

    @Test
    void passwordsDoNotRepeat() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            seen.add(generator.generate());
        }

        assertThat(seen).hasSize(500);
    }

    @Test
    void passwordsFitInsideBcryptsSeventyTwoByteLimit() {
        assertThat(generator.generate().getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThanOrEqualTo(72);
    }
}
