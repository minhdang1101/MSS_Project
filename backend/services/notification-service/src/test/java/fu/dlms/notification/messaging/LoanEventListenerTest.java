package fu.dlms.notification.messaging;

import fu.dlms.notification.entity.NotificationType;
import fu.dlms.notification.service.NotificationService;
import fu.dlms.notification.service.Recipient;
import fu.dlms.notification.service.RecipientResolver;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử LoanEventListener. Service và RecipientResolver được giả lập bằng Mockito nên không cần RabbitMQ,
 * database hay member-service: gọi thẳng hàm onLoanEvent như thể RabbitMQ vừa giao một message.
 */
class LoanEventListenerTest {

    private NotificationService service;
    private RecipientResolver resolver;
    private LoanEventListener listener;

    private final Recipient email = Recipient.email("a@x.com");

    @BeforeEach
    void setUp() {
        service = mock(NotificationService.class);
        resolver = mock(RecipientResolver.class);
        when(resolver.resolve(anyLong())).thenReturn(email);
        listener = new LoanEventListener(service, resolver);
    }

    private static LoanEvent event(Long loanId, Long memberId, Instant dueAt) {
        return new LoanEvent("evt-1", "LoanDueSoon", loanId, memberId, 3L, 7L, "PHYSICAL", null, dueAt, null,
                Instant.now());
    }

    @Test
    @DisplayName("Routing key quyết định loại thông báo")
    void routingKeyDecidesType() {
        assertThat(LoanEventListener.toNotificationType("loan.due-soon")).isEqualTo(NotificationType.DUE_SOON);
        assertThat(LoanEventListener.toNotificationType("loan.overdue")).isEqualTo(NotificationType.OVERDUE);
        assertThat(LoanEventListener.toNotificationType("loan.created")).isNull();
    }

    @Test
    @DisplayName("Sự kiện mới: hỏi người nhận rồi chuyển cho service lưu")
    void dueSoonEventIsPassedToService() {
        LoanEvent e = event(15L, 1L, Instant.now());
        when(service.createReminder(NotificationType.DUE_SOON, e, email)).thenReturn(ReminderResult.CREATED);

        listener.onLoanEvent(e, "loan.due-soon");

        verify(resolver).resolve(1L);
        verify(service).createReminder(NotificationType.DUE_SOON, e, email);
    }

    @Test
    @DisplayName("Thông báo đã có: dừng sớm, không gọi member-service, không lưu")
    void duplicateSkipsMemberLookup() {
        LoanEvent e = event(15L, 1L, Instant.now());
        when(service.reminderExists(NotificationType.OVERDUE, 15L)).thenReturn(true);

        listener.onLoanEvent(e, "loan.overdue");

        verify(resolver, never()).resolve(anyLong());
        verify(service, never()).createReminder(any(), any(), any());
    }

    @Test
    @DisplayName("Routing key lạ: bỏ qua, không gọi service")
    void unexpectedRoutingKeyIsIgnored() {
        listener.onLoanEvent(event(15L, 1L, Instant.now()), "loan.created");

        verify(service, never()).createReminder(any(), any(), any());
    }

    @Test
    @DisplayName("Thiếu dữ liệu bắt buộc: reject, không thử lại (đi vào dead letter queue)")
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
        // Lần kiểm tra đầu: chưa có. Sau khi INSERT bị chặn: đã có (bản sao kia vừa lưu)
        when(service.reminderExists(NotificationType.DUE_SOON, 15L)).thenReturn(false, true);
        when(service.createReminder(NotificationType.DUE_SOON, e, email))
                .thenThrow(new DataIntegrityViolationException("dup"));

        assertThatCode(() -> listener.onLoanEvent(e, "loan.due-soon")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Lỗi database khác (không phải trùng khóa) vẫn được ném ra, không bị che giấu")
    void otherConstraintErrorIsNotHidden() {
        LoanEvent e = event(15L, 1L, Instant.now());
        when(service.reminderExists(NotificationType.DUE_SOON, 15L)).thenReturn(false);
        when(service.createReminder(NotificationType.DUE_SOON, e, email))
                .thenThrow(new DataIntegrityViolationException("null column"));

        assertThatThrownBy(() -> listener.onLoanEvent(e, "loan.due-soon"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
