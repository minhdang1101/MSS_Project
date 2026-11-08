package fu.dlms.notification.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTypeTest {

    @Test
    void keyIsTypeAndLoanId() {
        assertThat(NotificationType.DUE_SOON.idempotencyKey(15L)).isEqualTo("DUE_SOON:15");
        assertThat(NotificationType.OVERDUE.idempotencyKey(15L)).isEqualTo("OVERDUE:15");
    }

    @Test
    void sameLoanDifferentTypeGivesDifferentKeys() {
        assertThat(NotificationType.DUE_SOON.idempotencyKey(15L))
                .isNotEqualTo(NotificationType.OVERDUE.idempotencyKey(15L));
    }

    @Test
    void missingLoanIdIsRejected() {
        assertThatThrownBy(() -> NotificationType.DUE_SOON.idempotencyKey(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
