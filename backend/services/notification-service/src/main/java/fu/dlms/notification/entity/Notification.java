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
 * Một thông báo nhắc hạn cho một lượt mượn.
 *
 * <p>Vòng đời: LoanEventListener tạo ở trạng thái PENDING → NotificationDispatcher gửi → SENT, hoặc FAILED sau
 * {@link #MAX_ATTEMPTS} lần gửi lỗi. created_at / updated_at được kế thừa từ BaseEntity.
 *
 * <p>Cấu trúc bảng phải khớp với docker/postgres/01-schema.sql (script tạo database khi chạy Docker).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification", indexes = {
        // Cùng tên với 01-schema.sql. Ở đó idx_notification_pending là partial index (WHERE status = 'PENDING');
        // Hibernate không tạo được partial index nên khi Hibernate tự tạo bảng thì đây là index thường.
        // Dùng cho NotificationDispatcher: tìm các thông báo PENDING đã đến giờ gửi.
        @Index(name = "idx_notification_pending", columnList = "scheduled_at"),
        // Danh sách thông báo của một độc giả, mới nhất trước.
        @Index(name = "idx_notification_member_scheduled", columnList = "member_id, scheduled_at DESC")
})
public class Notification extends BaseEntity {

    /** Số lần gửi tối đa; gửi lỗi đủ số lần này thì chuyển sang FAILED. */
    public static final int MAX_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Khóa chống trùng dạng "{loại}:{loanId}", vd "DUE_SOON:15". Cột UNIQUE nên mỗi lượt mượn có tối đa một thông báo
     * mỗi loại, kể cả khi cùng một sự kiện được giao nhiều lần. Tạo bằng {@link NotificationType#idempotencyKey(Long)}.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /** eventId của sự kiện đã tạo ra dòng này. Chỉ để truy vết, không dùng để kiểm tra trùng. */
    @Column(name = "source_event_id", length = 36)
    private String sourceEventId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "loan_id")
    private Long loanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    /** EMAIL nếu độc giả có email, ngược lại IN_APP (xem RecipientResolver). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    /** Địa chỉ email, hoặc "member:{id}" với kênh IN_APP. */
    @Column(nullable = false, length = 150)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Thời điểm sớm nhất được phép gửi. Gửi lỗi thì được dời ra sau để thử lại. */
    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    /** Số lần đã thử gửi. */
    @Column(nullable = false)
    private int attempts = 0;

    /** Lý do của lần gửi lỗi gần nhất, để thủ thư xem khi thông báo bị FAILED. */
    @Column(name = "last_error", length = 500)
    private String lastError;

    /** Lúc độc giả mở thông báo trong ứng dụng; null nghĩa là chưa đọc. */
    @Column(name = "read_at")
    private Instant readAt;
}
