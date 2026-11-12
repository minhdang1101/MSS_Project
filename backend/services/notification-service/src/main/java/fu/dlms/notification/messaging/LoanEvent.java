package fu.dlms.notification.messaging;

import java.time.Instant;

/**
 * Payload loan-service phát mỗi khi lượt mượn đổi trạng thái (xem fu.dlms.loan.messaging.LoanEvent bên loan-service).
 * Hai bên phải giữ cùng tên trường; JSON có trường lạ thì Jackson bỏ qua, thiếu trường thì giá trị là null.
 * eventType có dạng "LoanDueSoon", "LoanOverdue"... nhưng notification-service xác định loại theo routing key.
 */
public record LoanEvent(
        String eventId,
        String eventType,
        Long loanId,
        Long memberId,
        Long bookId,
        Long copyId,
        String loanType,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        Instant occurredAt
) {
}
