package fu.dlms.notification.entity;

/** Loại thông báo nhắc hạn. */
public enum NotificationType {
    /** Sách sắp đến hạn trả (routing key loan.due-soon). */
    DUE_SOON,
    /** Sách đã quá hạn trả (routing key loan.overdue). */
    OVERDUE;

    /**
     * Khóa chống trùng theo nghiệp vụ: "DUE_SOON:15" = thông báo sắp hạn của lượt mượn 15.
     *
     * <p>Dùng loanId thay vì eventId: cùng một lời nhắc có thể đến với eventId khác nhau (vd loan-service quét lại sau
     * khi khôi phục dữ liệu, hoặc ai đó gửi lại sự kiện bằng tay), nhưng độc giả chỉ nên nhận một thông báo mỗi loại
     * cho mỗi lượt mượn. Cùng định dạng với dữ liệu mẫu trong docker/postgres/02-seed.sql.
     */
    public String idempotencyKey(Long loanId) {
        if (loanId == null) {
            throw new IllegalArgumentException("loanId is required to build an idempotency key");
        }
        return name() + ":" + loanId;
    }
}
