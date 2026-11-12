package fu.dlms.notification.service;

import fu.dlms.notification.entity.NotificationType;
import fu.dlms.notification.messaging.LoanEvent;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Tạo tiêu đề và nội dung cho từng loại thông báo.
 *
 * <p>Tách riêng khỏi service để khi cần đổi câu chữ (hoặc sau này đọc mẫu từ file / database) thì chỉ sửa một chỗ.
 */
@Component
public class NotificationTemplates {

    /**
     * Thời gian trong sự kiện là Instant (giờ UTC). Định dạng theo giờ Việt Nam để độc giả đọc đúng,
     * ví dụ 02:00 UTC hiển thị thành "09:00 06/11/2026".
     */
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    /** Tiêu đề thông báo (dùng làm tiêu đề email ở Task N4/N5). */
    public String subject(NotificationType type) {
        return switch (type) {
            case DUE_SOON -> "Sách sắp đến hạn trả";
            case OVERDUE -> "Sách đã quá hạn trả";
        };
    }

    /**
     * Nội dung thông báo, gồm mã lượt mượn, mã sách và hạn trả.
     * Sự kiện chỉ có mã sách (bookId), chưa có tên sách vì notification-service không gọi sang book-catalog-service.
     */
    public String message(NotificationType type, LoanEvent event) {
        String dueAt = DATE_TIME.format(event.dueAt());
        return switch (type) {
            case DUE_SOON -> "Lượt mượn #%d (sách #%d) sẽ đến hạn trả lúc %s. Vui lòng trả sách đúng hạn để tránh bị phạt."
                    .formatted(event.loanId(), event.bookId(), dueAt);
            case OVERDUE -> "Lượt mượn #%d (sách #%d) đã quá hạn trả từ %s. Vui lòng trả sách sớm nhất có thể; tiền phạt được tính theo số ngày trễ."
                    .formatted(event.loanId(), event.bookId(), dueAt);
        };
    }
}
