package fu.dlms.notification.service.impl;

import fu.dlms.notification.dto.res.NotificationResponse;
import fu.dlms.notification.entity.Notification;
import fu.dlms.notification.entity.NotificationChannel;
import fu.dlms.notification.entity.NotificationStatus;
import fu.dlms.notification.entity.NotificationType;
import fu.dlms.notification.exception.BusinessException;
import fu.dlms.notification.exception.ResourceNotFoundException;
import fu.dlms.notification.messaging.LoanEvent;
import fu.dlms.notification.repository.NotificationRepository;
import fu.dlms.notification.service.NotificationTemplates;
import fu.dlms.notification.service.Recipient;
import fu.dlms.notification.service.ReminderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private static final Instant NOW = Instant.parse("2026-11-04T01:00:00Z");

    private NotificationRepository repository;
    private NotificationServiceImpl service;

    private final LoanEvent event = new LoanEvent("evt-1", "LoanDueSoon", 15L, 1L, 3L, 7L, "PHYSICAL",
            Instant.parse("2026-10-23T02:00:00Z"), Instant.parse("2026-11-06T02:00:00Z"), null, NOW);

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        service = new NotificationServiceImpl(repository, new NotificationTemplates(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Notification stored(Long id, Long memberId, NotificationStatus status) {
        Notification n = new Notification();
        n.setId(id);
        n.setMemberId(memberId);
        n.setStatus(status);
        n.setType(NotificationType.DUE_SOON);
        n.setChannel(NotificationChannel.EMAIL);
        return n;
    }

    @Test
    @DisplayName("Sự kiện mới: lưu thông báo PENDING với đúng khóa, kênh, người nhận và nội dung")
    void newEventSavesPendingNotification() {
        when(repository.existsByIdempotencyKey("DUE_SOON:15")).thenReturn(false);

        ReminderResult result = service.createReminder(NotificationType.DUE_SOON, event, Recipient.email("a@x.com"));

        assertThat(result).isEqualTo(ReminderResult.CREATED);
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).saveAndFlush(saved.capture());
        Notification n = saved.getValue();
        assertThat(n.getIdempotencyKey()).isEqualTo("DUE_SOON:15");
        assertThat(n.getSourceEventId()).isEqualTo("evt-1");
        assertThat(n.getMemberId()).isEqualTo(1L);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(n.getRecipient()).isEqualTo("a@x.com");
        assertThat(n.getScheduledAt()).isEqualTo(NOW);
        // dueAt 02:00 giờ UTC phải hiển thị thành 09:00 giờ Việt Nam
        assertThat(n.getMessage()).contains("#15").contains("09:00 06/11/2026");
    }

    @Test
    @DisplayName("Không có email: lưu với kênh IN_APP và người nhận member:{id}")
    void inAppRecipientIsStored() {
        when(repository.existsByIdempotencyKey("DUE_SOON:15")).thenReturn(false);

        service.createReminder(NotificationType.DUE_SOON, event, Recipient.inApp(1L));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(saved.getValue().getRecipient()).isEqualTo("member:1");
    }

    @Test
    @DisplayName("Khóa đã tồn tại: trả về DUPLICATE và không lưu gì")
    void existingKeyIsDuplicateAndSavesNothing() {
        when(repository.existsByIdempotencyKey("DUE_SOON:15")).thenReturn(true);

        ReminderResult result = service.createReminder(NotificationType.DUE_SOON, event, Recipient.inApp(1L));

        assertThat(result).isEqualTo(ReminderResult.DUPLICATE);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Sự kiện quá hạn dùng khóa OVERDUE riêng, không đụng khóa DUE_SOON")
    void overdueUsesItsOwnKey() {
        when(repository.existsByIdempotencyKey("OVERDUE:15")).thenReturn(false);

        service.createReminder(NotificationType.OVERDUE, event, Recipient.inApp(1L));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getIdempotencyKey()).isEqualTo("OVERDUE:15");
        assertThat(saved.getValue().getSubject()).isEqualTo("Sách đã quá hạn trả");
    }

    @Test
    @DisplayName("Gửi lại thông báo FAILED: về PENDING, attempts = 0, xóa lỗi cũ")
    void retryResetsFailedNotification() {
        Notification failed = stored(9L, 1L, NotificationStatus.FAILED);
        failed.setAttempts(3);
        failed.setLastError("SMTP down");
        when(repository.findById(9L)).thenReturn(Optional.of(failed));

        NotificationResponse response = service.retry(9L);

        assertThat(response.status()).isEqualTo(NotificationStatus.PENDING);
        assertThat(failed.getAttempts()).isZero();
        assertThat(failed.getLastError()).isNull();
        assertThat(failed.getScheduledAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Không gửi lại được thông báo không ở trạng thái FAILED (409)")
    void retryRejectsNonFailed() {
        when(repository.findById(9L)).thenReturn(Optional.of(stored(9L, 1L, NotificationStatus.SENT)));

        assertThatThrownBy(() -> service.retry(9L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Độc giả đánh dấu đã đọc thông báo của mình")
    void memberMarksOwnNotificationRead() {
        Notification mine = stored(9L, 1L, NotificationStatus.SENT);
        when(repository.findById(9L)).thenReturn(Optional.of(mine));

        NotificationResponse response = service.markRead(9L, 1L);

        assertThat(response.readAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Độc giả không đọc được thông báo của người khác (404, không lộ là có tồn tại)")
    void memberCannotReadOthersNotification() {
        when(repository.findById(9L)).thenReturn(Optional.of(stored(9L, 2L, NotificationStatus.SENT)));

        assertThatThrownBy(() -> service.markRead(9L, 1L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
