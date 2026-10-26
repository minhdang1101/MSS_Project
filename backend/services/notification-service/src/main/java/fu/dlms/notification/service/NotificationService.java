package fu.dlms.notification.service;

import fu.dlms.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> findAll(Long memberId);
}
