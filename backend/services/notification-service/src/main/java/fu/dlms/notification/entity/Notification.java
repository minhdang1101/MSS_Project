package fu.dlms.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One reminder for one loan. Created PENDING by the RabbitMQ consumer, then sent by NotificationDispatcher.
 * created_at / updated_at come from BaseEntity.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification", indexes = {
        // NotificationDispatcher looks up "PENDING and due" rows every minute
        @Index(name = "idx_notification_status_scheduled", columnList = "status, scheduled_at"),
        // GET /api/notifications?memberId=...
        @Index(name = "idx_notification_member", columnList = "member_id")
})
public class Notification extends BaseEntity {

    /** A reminder is retried at most this many times before it is marked FAILED. */
    public static final int MAX_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * "{type}:{loanId}", e.g. "DUE_SOON:15". UNIQUE, so one loan gets at most one reminder of each type,
     * even if the same event is delivered twice or loan-service emits it again the next day.
     * Built by {@link NotificationType#idempotencyKey(Long)}.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /** eventId of the LoanEvent that created this row. Only for tracing; not used for duplicate checks. */
    @Column(name = "source_event_id", length = 36)
    private String sourceEventId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "loan_id")
    private Long loanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false, length = 150)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Earliest time the dispatcher may send it. */
    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    /** How many times sending has been tried. */
    @Column(nullable = false)
    private int attempts = 0;

    /** Reason of the last failed send, shown when checking FAILED reminders. */
    @Column(name = "last_error", length = 500)
    private String lastError;
}
