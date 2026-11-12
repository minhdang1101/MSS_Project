package fu.dlms.notification.service;

import fu.dlms.notification.dto.res.NotificationResponse;
import fu.dlms.notification.entity.NotificationStatus;
import fu.dlms.notification.entity.NotificationType;
import fu.dlms.notification.messaging.LoanEvent;

import java.util.List;

/** Nghiệp vụ thông báo: tạo thông báo nhắc hạn, xem danh sách, đánh dấu đã đọc, gửi lại. */
public interface NotificationService {

    // ===================== Dành cho thủ thư (LIBRARIAN) =====================

    /**
     * Danh sách thông báo, mới nhất trước.
     *
     * @param memberId lọc theo độc giả; null = tất cả
     * @param status   lọc theo trạng thái; null = tất cả
     */
    List<NotificationResponse> findAll(Long memberId, NotificationStatus status);

    /** Chi tiết một thông báo; không có thì ném ResourceNotFoundException (404). */
    NotificationResponse findById(Long id);

    /**
     * Đưa một thông báo FAILED về PENDING để NotificationDispatcher gửi lại từ đầu (attempts = 0).
     * Thông báo không ở trạng thái FAILED thì ném BusinessException (409).
     */
    NotificationResponse retry(Long id);

    // ===================== Dành cho độc giả (MEMBER) =====================

    /** Thông báo của chính độc giả đang đăng nhập (memberId lấy từ JWT, do API Gateway truyền xuống). */
    List<NotificationResponse> findMine(Long memberId);

    /**
     * Đánh dấu đã đọc. Thông báo không thuộc độc giả này thì trả 404 như thể không tồn tại,
     * để không lộ ra là có thông báo đó.
     */
    NotificationResponse markRead(Long id, Long memberId);

    // ===================== Dành cho LoanEventListener =====================

    /**
     * Lưu một thông báo PENDING cho lượt mượn trong sự kiện, trừ khi lượt mượn đó đã có thông báo cùng loại.
     *
     * <p>Lưu ý: khi 2 bản sao của cùng một sự kiện được xử lý cùng lúc, hàm này vẫn có thể ném
     * DataIntegrityViolationException (database chặn bản ghi trùng). Nơi gọi phải bắt lỗi đó và coi là trùng.
     *
     * @param recipient gửi qua kênh nào, tới đâu (do RecipientResolver quyết định trước khi gọi)
     * @return CREATED nếu đã lưu thông báo mới, DUPLICATE nếu đã có từ trước
     */
    ReminderResult createReminder(NotificationType type, LoanEvent event, Recipient recipient);

    /** Kiểm tra lượt mượn đã có thông báo loại này chưa (dựa trên khóa chống trùng). */
    boolean reminderExists(NotificationType type, Long loanId);
}
