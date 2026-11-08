package fu.dlms.notification.service;

import fu.dlms.notification.dto.NotificationResponse;
import fu.dlms.notification.entity.NotificationType;
import fu.dlms.notification.messaging.LoanEvent;

import java.util.List;

/** Nghiệp vụ thông báo: xem danh sách và tạo thông báo nhắc hạn trả sách. */
public interface NotificationService {

    /**
     * Lấy danh sách thông báo.
     *
     * @param memberId lọc theo độc giả; null thì lấy tất cả
     */
    List<NotificationResponse> findAll(Long memberId);

    /**
     * Lưu một thông báo PENDING (chờ gửi) cho lượt mượn trong sự kiện, trừ khi lượt mượn đó đã có thông báo
     * cùng loại.
     *
     * <p>Lưu ý: khi 2 bản sao của cùng một sự kiện được xử lý cùng lúc, hàm này vẫn có thể ném
     * DataIntegrityViolationException (database chặn bản ghi trùng). Nơi gọi phải bắt lỗi đó và coi là trùng.
     *
     * @return CREATED nếu đã lưu thông báo mới, DUPLICATE nếu đã có từ trước
     */
    ReminderResult createReminder(NotificationType type, LoanEvent event);

    /** Kiểm tra lượt mượn đã có thông báo loại này chưa (dựa trên khóa chống trùng). */
    boolean reminderExists(NotificationType type, Long loanId);
}
