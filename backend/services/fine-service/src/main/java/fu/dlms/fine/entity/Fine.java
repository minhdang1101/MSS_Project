package fu.dlms.fine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One fine per loan: the unique loan_id makes duplicate LoanReturned/LoanOverdue events harmless.
 * loan_id, member_id and book_id are references to other services (no FK).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fine")
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false, unique = true)
    private Long loanId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    /** The rules this fine is calculated with, fixed when the fine is opened. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private FinePolicy policy;

    /** Days late (closed days excluded) before the grace period is taken off. Stale while ACCRUING. */
    @Column(name = "overdue_days", nullable = false)
    private int overdueDays;

    /** Snapshot of the policy rate at calculation time. */
    @Column(name = "daily_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyRate;

    /** Stale while ACCRUING: the API recalculates it up to today on every read. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FineStatus status = FineStatus.UNPAID;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    /** Optimistic lock: a librarian settling a fine must not race with the LoanReturned consumer. */
    @Version
    private Long version;
}
