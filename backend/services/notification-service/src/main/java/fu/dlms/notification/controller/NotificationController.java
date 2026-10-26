package fu.dlms.notification.controller;

import fu.dlms.notification.dto.NotificationResponse;
import fu.dlms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> findAll(@RequestParam(required = false) Long memberId) {
        return notificationService.findAll(memberId);
    }
}
