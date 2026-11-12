package fu.dlms.notification.scheduler;

import fu.dlms.notification.entity.Notification;
import fu.dlms.notification.entity.NotificationChannel;
import fu.dlms.notification.entity.NotificationStatus;
import fu.dlms.notification.repository.NotificationRepository;
import fu.dlms.notification.sender.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Kiểm thử N5: gửi một thông báo, cập nhật trạng thái, hẹn gửi lại khi lỗi. */
class NotificationDeliveryTest {

    private static final Instant NOW = Instant.parse("2026-11-04T01:00:00Z");
    private static final long RETRY_DELAY_MS = 60_000;

    private NotificationRepository repository;
    private NotificationSender sender;
    private NotificationDelivery delivery;
    private Notification notification;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        sender = mock(NotificationSender.class);
        delivery = new NotificationDelivery(repository, sender, Clock.fixed(NOW, ZoneOffset.UTC), RETRY_DELAY_MS);

        notification = new Notification();
        notification.setId(1L);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setRecipient("a@x.com");
        notification.setStatus(NotificationStatus.PENDING);
        notification.setScheduledAt(NOW);
        when(repository.findById(1L)).thenReturn(Optional.of(notification));
    }

    @Test
    @DisplayName("Gửi thành công: SENT, có sentAt, attempts = 1")
    void successMarksSent() {
        assertThat(delivery.deliver(1L)).isEqualTo(NotificationStatus.SENT);

        assertThat(notification.getSentAt()).isEqualTo(NOW);
        assertThat(notification.getAttempts()).isEqualTo(1);
        assertThat(notification.getLastError()).isNull();
    }

    @Test
    @DisplayName("Gửi lỗi lần 1: vẫn PENDING, lưu lý do, hẹn thử lại sau 1 x retryDelay")
    void firstFailureSchedulesRetry() {
        doThrow(new MailSendException("SMTP down")).when(sender).send(any());

        assertThat(delivery.deliver(1L)).isEqualTo(NotificationStatus.PENDING);

        assertThat(notification.getAttempts()).isEqualTo(1);
        assertThat(notification.getLastError()).contains("SMTP down");
        assertThat(notification.getScheduledAt()).isEqualTo(NOW.plusMillis(RETRY_DELAY_MS));
    }

    @Test
    @DisplayName("Gửi lỗi lần thứ 3 (MAX_ATTEMPTS): chuyển sang FAILED")
    void thirdFailureMarksFailed() {
        notification.setAttempts(Notification.MAX_ATTEMPTS - 1);
        doThrow(new MailSendException("SMTP down")).when(sender).send(any());

        assertThat(delivery.deliver(1L)).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getAttempts()).isEqualTo(Notification.MAX_ATTEMPTS);
    }

    @Test
    @DisplayName("Thông báo không còn PENDING (đã gửi ở lượt khác): bỏ qua, không gửi lần nữa")
    void notPendingIsSkipped() {
        notification.setStatus(NotificationStatus.SENT);

        assertThat(delivery.deliver(1L)).isNull();
        verify(sender, never()).send(any());
    }
}
