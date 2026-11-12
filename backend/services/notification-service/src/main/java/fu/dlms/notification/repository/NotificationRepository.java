package fu.dlms.notification.repository;

import fu.dlms.notification.entity.Notification;
import fu.dlms.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Truy cập bảng notification. Spring Data JPA tự sinh câu SQL từ tên hàm,
 * vd findByMemberIdOrderByScheduledAtDesc → WHERE member_id = ? ORDER BY scheduled_at DESC.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Khóa chống trùng đã có chưa (lớp chống trùng 1). */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /** Thông báo của một độc giả, mới nhất trước. */
    List<Notification> findByMemberIdOrderByScheduledAtDesc(Long memberId);

    /**
     * Danh sách cho thủ thư, lọc tùy chọn theo độc giả và trạng thái (tham số null = không lọc), mới nhất trước.
     * Viết bằng @Query vì tên hàm tự sinh không diễn tả được "null thì bỏ qua điều kiện".
     */
    @Query("""
            select n from Notification n
            where (:memberId is null or n.memberId = :memberId)
              and (:status is null or n.status = :status)
            order by n.scheduledAt desc, n.id desc
            """)
    List<Notification> search(@Param("memberId") Long memberId, @Param("status") NotificationStatus status);

    /**
     * Id các thông báo đang chờ và đã đến giờ gửi, cũ nhất trước; tối đa 50 mỗi lượt để một lần quét không quá lâu.
     * Chỉ lấy id: mỗi thông báo được nạp lại và gửi trong transaction riêng (xem NotificationDelivery).
     */
    @Query("""
            select n.id from Notification n
            where n.status = :status and n.scheduledAt <= :now
            order by n.scheduledAt asc
            limit 50
            """)
    List<Long> findDueIds(@Param("status") NotificationStatus status, @Param("now") Instant now);
}
