package fu.dlms.notification.service;

import fu.dlms.notification.entity.NotificationChannel;

/**
 * Gửi thông báo qua kênh nào, tới địa chỉ nào.
 *
 * @param channel EMAIL (có email) hoặc IN_APP (chỉ xem trong ứng dụng)
 * @param address địa chỉ email, hoặc "member:{id}" với IN_APP
 */
public record Recipient(NotificationChannel channel, String address) {

    public static Recipient email(String email) {
        return new Recipient(NotificationChannel.EMAIL, email);
    }

    public static Recipient inApp(Long memberId) {
        return new Recipient(NotificationChannel.IN_APP, "member:" + memberId);
    }
}
