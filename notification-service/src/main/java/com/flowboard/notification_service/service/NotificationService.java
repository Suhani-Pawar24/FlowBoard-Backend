package com.flowboard.notification_service.service;

import com.flowboard.notification_service.entity.Notification;
import java.util.List;

/**
 * NotificationService — Case Study Section 4.8
 *
 * Declares: send(), sendBulk(), markAsRead(), markAllRead(),
 * deleteRead(), getByRecipient(), getUnreadCount(),
 * deleteNotification(), getAll()
 */
public interface NotificationService {
    Notification send(Notification notification);
    void sendBulk(List<Notification> notifications);
    List<Notification> getByRecipient(Long recipientId);
    List<Notification> getUnreadByRecipient(Long recipientId);
    Notification markAsRead(Long notificationId);
    void markAllRead(Long recipientId);
    void deleteRead(Long recipientId);
    void deleteNotification(Long notificationId);
    long getUnreadCount(Long recipientId);
    List<Notification> getAll();
}
