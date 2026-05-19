package com.flowboard.notification_service.controller;

import com.flowboard.notification_service.entity.Notification;
import com.flowboard.notification_service.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * NotificationResource — Case Study Section 4.8
 *
 * Exposes: /notifications endpoints:
 *   POST /                           — send single notification
 *   POST /bulk                       — send bulk notifications
 *   GET  /recipient/{recipientId}    — get all notifications for user
 *   GET  /recipient/{recipientId}/unread       — get unread
 *   GET  /recipient/{recipientId}/unread/count  — get unread count
 *   PUT  /{id}/read                  — mark single as read
 *   PUT  /recipient/{recipientId}/read-all     — mark all as read
 *   DELETE /recipient/{recipientId}/read       — delete all read notifications
 *   DELETE /{id}                     — delete single
 *   GET  /all                        — admin: get all notifications
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Notification> send(@RequestBody Notification notification) {
        return ResponseEntity.ok(notificationService.send(notification));
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> sendBulk(@RequestBody List<Notification> notifications) {
        try {
            notificationService.sendBulk(notifications);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getClass().getName(), "message", e.getMessage()));
        }
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Notification>> getByRecipient(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getByRecipient(recipientId));
    }

    @GetMapping("/recipient/{recipientId}/unread")
    public ResponseEntity<List<Notification>> getUnread(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getUnreadByRecipient(recipientId));
    }

    @GetMapping("/recipient/{recipientId}/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long recipientId) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(recipientId)));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    public ResponseEntity<Void> markAllRead(@PathVariable Long recipientId) {
        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/recipient/{recipientId}/read")
    public ResponseEntity<Void> deleteRead(@PathVariable Long recipientId) {
        notificationService.deleteRead(recipientId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }
}
