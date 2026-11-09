package fu.dlms.notification.client;

/**
 * Phần dữ liệu notification-service cần từ MemberResponse của member-service.
 * Các trường khác trong JSON trả về bị bỏ qua (Jackson không báo lỗi trường thừa).
 */
public record MemberView(
        Long id,
        String fullName,
        String email,
        String phone,
        String status
) {
}
