package fu.dlms.notification.messaging;

import fu.dlms.notification.entity.NotificationType;
import fu.dlms.notification.service.NotificationService;
import fu.dlms.notification.service.ReminderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử LoanEventListener. Service được giả lập bằng Mockito nên không cần RabbitMQ hay database:
 * gọi thẳng hàm onLoanEvent như thể RabbitMQ vừa giao một message.
 */
class LoanEventListenerTest {

    private NotificationService service;
    private LoanEventListener listener;

    @BeforeEach
    void setUp() {
        service = mock(NotificationService.class);
        listener = new LoanEventListener(service);
    }

    private static LoanEvent event(Long loanId, Long memberId, Instant dueAt) {
        return new LoanEvent("evt-1", "ANY", loanId, memberId, 3L, 7L, null, dueAt, null, Instant.now());
    }

    @Test
    @DisplayName("Routing key quyết định loại thông báo")
    void routingKeyDecidesType() {
        assertThat(LoanEventListener.toNotificationType("loan.due-soon")).isEqualTo(NotificationType.DUE_SOON);
        assertThat(LoanEventListener.toNotificationType("loan.overdue")).isEqualTo(NotificationType.OVERDUE);
        assertThat(LoanEventListener.toNotificationType("loan.created")).isNull();
    }

    @Test
    @DisplayName("Sự kiện sắp đến hạn được chuyển cho service xử lý")
    void dueSoonEventIsPassedToService() {
        LoanEvent e = event(15L, 1L, Instant.now());
        when(service.createReminder(NotificationType.DUE_SOON, e)).thenReturn(ReminderResult.CREATED);

        listener.onLoanEvent(e, "loan.due-soon");

        verify(service).createReminder(NotificationType.DUE_SOON, e);
    }

    @Test
    @DisplayName("Routing key lạ: bỏ qua, không gọi service")
    void unexpectedRoutingKeyIsIgnored() {
        listener.onLoanEvent(event(15L, 1L, Instant.now()), "loan.created");

        verify(service, never()).createReminder(any(), any());
    }

    @Test
    @DisplayName("Thiếu dữ liệu bắt buộc: loại bỏ message, không gửi lại")
    void missingFieldIsRejectedWithoutRequeue() {
        assertThatThrownBy(() -> listener.onLoanEvent(event(null, 1L, Instant.now()), "loan.due-soon"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("loanId");
        assertThatThrownBy(() -> listener.onLoanEvent(event(15L, 1L, null), "loan.overdue"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("dueAt");
    }

    @Test
    @DisplayName("2 bản sao xử lý cùng lúc: lỗi UNIQUE được coi là trùng, không ném lỗi")
    void uniqueKeyRaceIsTreatedAsDuplicate() {
        LoanEvent e = event(15L, 1L, Instant.now());
        when(service.createReminder(NotificationType.DUE_SOON, e)).thenThrow(new DataIntegrityViolationException("dup"));
        when(service.reminderExists(NotificationType.DUE_SOON, 15L)).thenReturn(true);

        assertThatCode(() -> listener.onLoanEvent(e, "loan.due-soon")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Lỗi database khác (không phải trùng khóa) vẫn được ném ra, không bị che giấu")
    void otherConstraintErrorIsNotHidden() {
        LoanEvent e = event(15L, 1L, Instant.now());
        when(service.createReminder(NotificationType.DUE_SOON, e)).thenThrow(new DataIntegrityViolationException("null column"));
        when(service.reminderExists(NotificationType.DUE_SOON, 15L)).thenReturn(false);

        assertThatThrownBy(() -> listener.onLoanEvent(e, "loan.due-soon"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
