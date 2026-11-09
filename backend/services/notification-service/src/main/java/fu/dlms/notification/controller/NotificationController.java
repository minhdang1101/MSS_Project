package fu.dlms.notification.controller;

import fu.dlms.notification.dto.NotificationResponse;
import fu.dlms.notification.entity.NotificationStatus;
import fu.dlms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API thông báo (Task N7). Mọi request đi qua API Gateway; Gateway đã kiểm tra JWT và role trước khi chuyển tới đây:
 * <ul>
 *   <li>LIBRARIAN: xem toàn bộ, xem chi tiết, gửi lại thông báo FAILED.</li>
 *   <li>MEMBER: chỉ xem và đánh dấu đã đọc thông báo của chính mình.</li>
 * </ul>
 * Service này không tự kiểm tra JWT. Danh tính người gọi đến từ header {@code X-Member-Id} do Gateway điền từ claim
 * "mid" trong token (Gateway luôn xóa header này nếu client tự gửi lên, nên không giả mạo được).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** LIBRARIAN. Vd GET /api/notifications?memberId=3&status=FAILED (cả hai tham số đều không bắt buộc). */
    @GetMapping
    public List<NotificationResponse> findAll(@RequestParam(required = false) Long memberId,
                                              @RequestParam(required = false) NotificationStatus status) {
        return notificationService.findAll(memberId, status);
    }

    /** MEMBER. Thông báo của chính độc giả đang đăng nhập. Khai báo trước /{id} cho dễ đọc. */
    @GetMapping("/me")
    public List<NotificationResponse> findMine(@RequestHeader("X-Member-Id") Long memberId) {
        return notificationService.findMine(memberId);
    }

    /** LIBRARIAN. */
    @GetMapping("/{id}")
    public NotificationResponse findById(@PathVariable Long id) {
        return notificationService.findById(id);
    }

    /** MEMBER. Đánh dấu đã đọc; chỉ được với thông báo của chính mình. */
    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id, @RequestHeader("X-Member-Id") Long memberId) {
        return notificationService.markRead(id, memberId);
    }

    /** LIBRARIAN. Đưa thông báo FAILED về PENDING để gửi lại. */
    @PostMapping("/{id}/retry")
    public NotificationResponse retry(@PathVariable Long id) {
        return notificationService.retry(id);
    }
}
