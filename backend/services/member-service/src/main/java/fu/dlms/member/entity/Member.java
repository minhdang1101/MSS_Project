package fu.dlms.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "member")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "library_card_no", nullable = false, unique = true, length = 30)
    private String libraryCardNo;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false)
    private LocalDate joinedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) not null default 'MEMBER'")
    private MemberRole role = MemberRole.MEMBER;

    @Column(name = "reset_code_hash", length = 100)
    private String resetCodeHash;

    @Column(name = "reset_code_expires_at")
    private Instant resetCodeExpiresAt;

    @Column(name = "reset_code_attempts", nullable = false, columnDefinition = "integer not null default 0")
    private int resetCodeAttempts;

    @Column(name = "reset_code_sent_at")
    private Instant resetCodeSentAt;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;
}
