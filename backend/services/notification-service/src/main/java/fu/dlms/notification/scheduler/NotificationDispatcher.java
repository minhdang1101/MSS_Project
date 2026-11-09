package fu.dlms.notification.scheduler;

import fu.dlms.notification.entity.NotificationStatus;
import fu.dlms.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Định kỳ gửi các thông báo PENDING đã đến giờ (Task N5 - reminder scheduling).
 *
 * <p>Phân chia trách nhiệm với loan-service:
 * <ul>
 *   <li>loan-service (DueDateScheduler, 8h sáng mỗi ngày) quyết định KHI NÀO cần nhắc: lượt mượn nào sắp đến hạn,
 *       lượt mượn nào đã quá hạn, rồi phát sự kiện.</li>
 *   <li>notification-service quyết định nhắc THẾ NÀO: nội dung, kênh gửi, gửi lại khi lỗi, chống trùng.
 *       Service này không tự đọc dữ liệu mượn sách.</li>
 * </ul>
 *
 * <p>{@code fixedDelay}: lần quét sau bắt đầu sau khi lần trước XONG một khoảng {@code dispatch-interval-ms}
 * (mặc định 60 giây), nên hai lần quét không bao giờ chạy chồng lên nhau trong cùng một instance.
 * Lưu ý: nếu chạy nhiều instance notification-service cùng lúc thì cần thêm khóa (vd ShedLock) để tránh gửi đôi;
 * dự án hiện chạy một instance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;
    private final NotificationDelivery delivery;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.notification.dispatch-interval-ms:60000}",
            initialDelayString = "${app.notification.dispatch-initial-delay-ms:10000}")
    public void dispatch() {
        List<Long> dueIds = notificationRepository.findDueIds(NotificationStatus.PENDING, clock.instant());
        if (dueIds.isEmpty()) {
            return;
        }
        Map<NotificationStatus, Integer> summary = new EnumMap<>(NotificationStatus.class);
        for (Long id : dueIds) {
            try {
                NotificationStatus result = delivery.deliver(id);
                if (result != null) {
                    summary.merge(result, 1, Integer::sum);
                }
            } catch (RuntimeException e) {
                // Lỗi ngoài dự kiến (vd mất kết nối database) ở một thông báo không được chặn các thông báo còn lại
                log.error("Could not process notification {}: {}", id, e.getMessage(), e);
            }
        }
        // PENDING trong summary = gửi lỗi, đã hẹn thử lại
        log.info("Dispatched {} due notification(s): {}", dueIds.size(), summary);
    }
}
