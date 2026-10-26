package fu.dlms.member.config;

import fu.dlms.member.entity.Librarian;
import fu.dlms.member.repository.LibrarianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Creates the first librarian from configuration when the table is empty, so there is a way to sign in.
 * Credentials come from app.bootstrap.librarian.* and are never hard-coded; leave the password empty to disable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LibrarianBootstrap implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72; // BCrypt ignores anything beyond this
    private static final int MAX_STAFF_CODE_LENGTH = 30;
    private static final int MAX_FULL_NAME_LENGTH = 150;
    private static final String PLACEHOLDER_PASSWORD = "change_me_before_first_run";

    private final LibrarianRepository librarianRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.librarian.staff-code:}")
    private String staffCode;

    @Value("${app.bootstrap.librarian.full-name:Thủ thư}")
    private String fullName;

    @Value("${app.bootstrap.librarian.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (librarianRepository.count() > 0 || staffCode.isBlank() || password.isBlank()) {
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH || PLACEHOLDER_PASSWORD.equals(password)
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            log.warn("Initial librarian NOT created: app.bootstrap.librarian.password must be {}-{} bytes and not the "
                    + "placeholder from the example file.", MIN_PASSWORD_LENGTH, MAX_PASSWORD_BYTES);
            return;
        }
        if (staffCode.trim().length() > MAX_STAFF_CODE_LENGTH || fullName.length() > MAX_FULL_NAME_LENGTH) {
            log.warn("Initial librarian NOT created: staff code must be at most {} and full name at most {} characters.",
                    MAX_STAFF_CODE_LENGTH, MAX_FULL_NAME_LENGTH);
            return;
        }
        Librarian librarian = new Librarian();
        librarian.setStaffCode(staffCode.trim());
        librarian.setFullName(fullName);
        librarian.setPasswordHash(passwordEncoder.encode(password));
        librarianRepository.save(librarian);
        log.info("Created initial librarian account {}", librarian.getStaffCode());
    }
}
