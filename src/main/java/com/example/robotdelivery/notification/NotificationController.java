package com.example.robotdelivery.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "通知记录", description = "短信/小程序取件通知记录")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Operation(summary = "按配送单查询通知记录")
    @GetMapping
    public List<Notification> byDelivery(@RequestParam Long deliveryId) {
        return notificationRepository.findByDeliveryId(deliveryId);
    }
}
