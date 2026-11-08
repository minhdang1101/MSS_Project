package fu.dlms.notification.service;

/** Kết quả xử lý một sự kiện "sắp đến hạn" / "quá hạn". */
public enum ReminderResult {
    /** Đã lưu một thông báo mới ở trạng thái PENDING (chờ gửi). */
    CREATED,
    /** Lượt mượn đã có thông báo cùng loại (cùng khóa chống trùng); không lưu gì thêm. */
    DUPLICATE
}
