package fu.dlms.member.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Initial passwords for readers. Twelve characters from an alphabet without look-alikes (no 0/O, 1/l/I), always with
 * at least one upper-case letter, one lower-case letter and one digit, drawn from a cryptographically secure source.
 */
@Component
public class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL = UPPER + LOWER + DIGITS;
    private static final int LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        char[] password = new char[LENGTH];
        password[0] = pick(UPPER);
        password[1] = pick(LOWER);
        password[2] = pick(DIGITS);
        for (int i = 3; i < LENGTH; i++) {
            password[i] = pick(ALL);
        }
        // Shuffle so the guaranteed characters are not always in the first three places.
        for (int i = LENGTH - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char swap = password[i];
            password[i] = password[j];
            password[j] = swap;
        }
        return new String(password);
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }
}
