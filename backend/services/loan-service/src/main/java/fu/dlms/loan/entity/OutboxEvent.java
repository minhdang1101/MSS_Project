package fu.dlms.loan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** Transactional outbox: written in the same transaction as the loan, relayed to RabbitMQ afterwards. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    /** UUID, also used as eventId by consumers for de-duplication. */
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private Instant publishedAt;
}
