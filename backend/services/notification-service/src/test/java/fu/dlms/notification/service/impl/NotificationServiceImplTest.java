package fu.dlms.notification.service.impl;

import fu.dlms.notification.entity.Notification;
import fu.dlms.notification.entity.NotificationChannel;
import fu.dlms.notification.entity.NotificationStatus;
import fu.dlms.notification.entity.NotificationType;
import fu.dlms.notification.messaging.LoanEvent;
import fu.dlms.notification.repository.NotificationRepository;
import fu.dlms.notification.service.NotificationTemplates;
import fu.dlms.notification.service.ReminderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử NotificationServiceImpl. Repository được giả lập bằng Mockito nên không cần database:
 * test tự quy định "khóa đã có hay chưa" rồi kiểm tra service có lưu đúng hay không.
 */
class NotificationServiceImplTest {

    private NotificationRepository repository;
    private NotificationServiceImpl service;

    private final LoanEvent event = new LoanEvent("evt-1", "LOAN_DUE_SOON", 15L, 1L, 3L, 7L,
            Instant.parse("2026-10-23T02:00:00Z"), Instant.parse("2026-11-06T02:00:00Z"), null,
            Instant.parse("2026-11-04T01:00:00Z"));

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        service = new NotificationServiceImpl(repository, new NotificationTemplates());
    }

    @Test
    @DisplayName("Sự kiện mới: lưu thông báo PENDING với đúng khóa, người nhận và nội dung")
    void newEventSavesPendingNotification() {
        when(repository.existsByIdempotencyKey("DUE_SOON:15")).thenReturn(false);

        ReminderResult result = service.createReminder(NotificationType.DUE_SOON, event);

        assertThat(result).isEqualTo(ReminderResult.CREATED);
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).saveAndFlush(saved.capture());
        Notification n = saved.getValue();
        assertThat(n.getIdempotencyKey()).isEqualTo("DUE_SOON:15");
        assertThat(n.getSourceEventId()).isEqualTo("evt-1");
        assertThat(n.getMemberId()).isEqualTo(1L);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(n.getRecipient()).isEqualTo("member:1");
        assertThat(n.getScheduledAt()).isNotNull();
        // dueAt 02:00 giờ UTC phải hiển thị thành 09:00 giờ Việt Nam
        assertThat(n.getMessage()).contains("#15").contains("09:00 06/11/2026");
    }

    @Test
    @DisplayName("Khóa đã tồn tại: trả về DUPLICATE và không lưu gì")
    void existingKeyIsDuplicateAndSavesNothing() {
        when(repository.existsByIdempotencyKey("DUE_SOON:15")).thenReturn(true);

        ReminderResult result = service.createReminder(NotificationType.DUE_SOON, event);

        assertThat(result).isEqualTo(ReminderResult.DUPLICATE);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Sự kiện quá hạn dùng khóa OVERDUE riêng, không đụng khóa DUE_SOON")
    void overdueUsesItsOwnKey() {
        when(repository.existsByIdempotencyKey("OVERDUE:15")).thenReturn(false);

        service.createReminder(NotificationType.OVERDUE, event);

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getIdempotencyKey()).isEqualTo("OVERDUE:15");
        assertThat(saved.getValue().getSubject()).isEqualTo("Sách đã quá hạn trả");
    }
}
