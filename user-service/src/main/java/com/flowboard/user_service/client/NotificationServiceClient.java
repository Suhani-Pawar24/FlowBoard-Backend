package com.flowboard.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import java.util.Map;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @PostMapping("/notifications/bulk")
    String sendBulkNotifications(@RequestBody List<Map<String, Object>> notifications);

    @PostMapping("/api/notifications/email/send")
    String sendEmail(@RequestBody Map<String, String> emailPayload);
}
