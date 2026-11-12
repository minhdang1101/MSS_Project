package fu.dlms.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Gọi member-service bằng OpenFeign: chỉ cần khai báo interface, Spring tự sinh code gửi HTTP.
 *
 * <p>{@code name = "member-service"} là tên service đăng ký trên Eureka. Feign hỏi Eureka địa chỉ thật của
 * member-service (vd 192.168.1.5:8082) rồi gọi thẳng, KHÔNG đi qua API Gateway: đây là lời gọi nội bộ giữa các
 * service nên không cần JWT.
 */
@FeignClient(name = "member-service", path = "/api/members")
public interface MemberClient {

    /** GET /api/members/{id} của member-service. Ném FeignException nếu lỗi (404, 5xx, không kết nối được...). */
    @GetMapping("/{id}")
    MemberView getMember(@PathVariable("id") Long id);
}
