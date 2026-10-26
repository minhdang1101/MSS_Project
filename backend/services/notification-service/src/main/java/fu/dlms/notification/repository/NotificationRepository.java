package fu.dlms.notification.repository;

import fu.dlms.notification.entity.Notification;
import fu.dlms.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<Notification> findByMemberId(Long memberId);

    List<Notification> findByStatusAndScheduledAtBefore(NotificationStatus status, Instant time);
}
