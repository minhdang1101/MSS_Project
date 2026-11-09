package fu.dlms.notification.service;

import fu.dlms.notification.client.MemberClient;
import fu.dlms.notification.client.MemberView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Quyết định gửi thông báo cho độc giả qua kênh nào (Task N4).
 *
 * <p>Sự kiện từ loan-service chỉ có memberId, không có email. Vì vậy hỏi member-service:
 * <ul>
 *   <li>Độc giả có email → kênh EMAIL, gửi tới email đó.</li>
 *   <li>Không có email, không tìm thấy độc giả, hoặc member-service đang lỗi / tắt → kênh IN_APP. Độc giả vẫn thấy
 *       thông báo khi mở ứng dụng, và việc xử lý sự kiện không bị chặn chỉ vì một service khác đang gặp sự cố.</li>
 * </ul>
 *
 * <p>Lời gọi này là đồng bộ (chờ HTTP trả lời), khác với RabbitMQ là bất đồng bộ. Thời gian chờ tối đa được giới hạn
 * bằng cấu hình Feign (connect-timeout / read-timeout) để một member-service chậm không làm treo consumer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipientResolver {

    private final MemberClient memberClient;

    public Recipient resolve(Long memberId) {
        try {
            MemberView member = memberClient.getMember(memberId);
            if (member != null && member.email() != null && !member.email().isBlank()) {
                return Recipient.email(member.email());
            }
            log.info("Member {} has no email; notification stays in-app", memberId);
        } catch (RuntimeException e) {
            // FeignException (404, 5xx), RetryableException (timeout, không kết nối được),
            // hoặc "No instances available" khi member-service chưa đăng ký với Eureka
            log.warn("Could not look up member {} ({}); notification stays in-app",
                    memberId, e.getClass().getSimpleName());
        }
        return Recipient.inApp(memberId);
    }
}
