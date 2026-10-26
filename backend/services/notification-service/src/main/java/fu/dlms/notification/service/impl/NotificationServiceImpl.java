package fu.dlms.notification.service.impl;

import fu.dlms.notification.dto.NotificationResponse;
import fu.dlms.notification.repository.NotificationRepository;
import fu.dlms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public List<NotificationResponse> findAll(Long memberId) {
        var notifications = memberId == null
                ? notificationRepository.findAll()
                : notificationRepository.findByMemberId(memberId);
        return notifications.stream().map(NotificationResponse::from).toList();
    }
}
