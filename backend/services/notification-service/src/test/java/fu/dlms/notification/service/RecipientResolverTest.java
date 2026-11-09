package fu.dlms.notification.service;

import feign.FeignException;
import feign.Request;
import fu.dlms.notification.client.MemberClient;
import fu.dlms.notification.client.MemberView;
import fu.dlms.notification.entity.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Kiểm thử N4: chọn kênh gửi dựa trên dữ liệu member-service (MemberClient được giả lập). */
class RecipientResolverTest {

    private MemberClient memberClient;
    private RecipientResolver resolver;

    @BeforeEach
    void setUp() {
        memberClient = mock(MemberClient.class);
        resolver = new RecipientResolver(memberClient);
    }

    @Test
    @DisplayName("Độc giả có email: gửi EMAIL tới email đó")
    void memberWithEmailGetsEmail() {
        when(memberClient.getMember(3L)).thenReturn(new MemberView(3L, "Ngọc", "ngoc@example.com", null, "ACTIVE"));

        assertThat(resolver.resolve(3L)).isEqualTo(new Recipient(NotificationChannel.EMAIL, "ngoc@example.com"));
    }

    @Test
    @DisplayName("Độc giả không có email: IN_APP")
    void memberWithoutEmailGetsInApp() {
        when(memberClient.getMember(3L)).thenReturn(new MemberView(3L, "Ngọc", " ", null, "ACTIVE"));

        assertThat(resolver.resolve(3L)).isEqualTo(Recipient.inApp(3L));
    }

    @Test
    @DisplayName("Không tìm thấy độc giả (404): IN_APP, không ném lỗi")
    void unknownMemberGetsInApp() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/members/3", Map.of(), null,
                StandardCharsets.UTF_8, null);
        when(memberClient.getMember(3L)).thenThrow(new FeignException.NotFound("not found", request, null, null));

        assertThat(resolver.resolve(3L)).isEqualTo(Recipient.inApp(3L));
    }

    @Test
    @DisplayName("member-service tắt / không có trên Eureka: IN_APP, không chặn việc xử lý sự kiện")
    void memberServiceDownGetsInApp() {
        when(memberClient.getMember(3L)).thenThrow(new IllegalStateException("No instances available for member-service"));

        assertThat(resolver.resolve(3L)).isEqualTo(Recipient.inApp(3L));
    }
}
