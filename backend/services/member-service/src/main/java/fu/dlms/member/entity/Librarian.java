package fu.dlms.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A staff account that can sign in to the management UI. Library patrons are {@link Member}s and never sign in. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "librarian")
public class Librarian extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_code", nullable = false, unique = true, length = 30)
    private String staffCode;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /** BCrypt hash - the plain password is never stored or logged. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;
}
