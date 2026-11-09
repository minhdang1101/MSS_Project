package fu.dlms.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bảng định tuyến của API Gateway (Task G1): request có đường dẫn nào thì chuyển tới service nào.
 *
 * <p>Viết bằng Java thay vì trong application.properties vì file properties bị git-ignore (chứa mật khẩu). Route không
 * phải bí mật, đặt ở đây thì mọi máy và mọi container Docker đều có cùng một bảng route, không ai phải tự chép.
 *
 * <p>{@code lb://member-service}: "lb" = load balancer. Gateway hỏi Eureka danh sách instance đang chạy của
 * member-service rồi chọn một instance để chuyển request (chạy nhiều instance thì tự chia tải). Không instance nào
 * đăng ký thì Gateway trả 503.
 *
 * <p>Đường dẫn được giữ nguyên khi chuyển tiếp: /api/members/5 ở Gateway → /api/members/5 ở member-service.
 */
@Configuration
public class GatewayRoutesConfig {

    public static final String CATALOG = "lb://book-catalog-service";
    public static final String MEMBER = "lb://member-service";
    public static final String LOAN = "lb://loan-service";
    public static final String FINE = "lb://fine-service";
    public static final String NOTIFICATION = "lb://notification-service";

    @Bean
    public RouteLocator dlmsRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Đạt: sách, bản sao, ảnh bìa, giữ chỗ
                .route("books", r -> r.path("/api/books/**", "/api/copies/**").uri(CATALOG))
                // Minh: độc giả + đăng nhập / đổi mật khẩu
                .route("members", r -> r.path("/api/members/**").uri(MEMBER))
                .route("auth", r -> r.path("/api/auth/**").uri(MEMBER))
                // An: mượn / trả
                .route("loans", r -> r.path("/api/loans/**").uri(LOAN))
                // Hiếu: tiền phạt, chính sách phạt, ngày nghỉ
                .route("fines", r -> r.path("/api/fines/**", "/api/fine-policies/**").uri(FINE))
                // Độc giả xem phiếu mượn / tiền phạt của chính mình (X-Member-Id lấy từ token)
                .route("me-loans", r -> r.path("/api/me/loans/**").uri(LOAN))
                .route("me-fines", r -> r.path("/api/me/fines/**").uri(FINE))
                // Thành: thông báo nhắc hạn
                .route("notifications", r -> r.path("/api/notifications/**").uri(NOTIFICATION))
                .build();
    }
}
