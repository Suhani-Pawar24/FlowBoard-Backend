package com.flowboard.notification_service.service.impl;

import com.flowboard.notification_service.entity.Notification;
import com.flowboard.notification_service.repository.NotificationRepository;
import com.flowboard.notification_service.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Override
    public Notification send(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/notifications." + saved.getRecipientId(), 
            Map.of("eventType", "NOTIFICATION_RECEIVED", "entityId", saved.getNotificationId()));
        return saved;
    }

    @Override
    public void sendBulk(List<Notification> notifications) {
        List<Notification> saved = notificationRepository.saveAll(notifications);
        for (Notification n : saved) {
            messagingTemplate.convertAndSend("/topic/notifications." + n.getRecipientId(),
                Map.of(
                    "eventType", "NOTIFICATION_RECEIVED", 
                    "entityId", n.getNotificationId() != null ? n.getNotificationId() : 0,
                    "type", n.getType() != null ? n.getType().name() : "",
                    "title", n.getTitle() != null ? n.getTitle() : "Broadcast",
                    "message", n.getMessage() != null ? n.getMessage() : ""
                ));
        }
    }

    @Override
    public List<Notification> getByRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    @Override
    public List<Notification> getUnreadByRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(recipientId, false);
    }

    @Override
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead(Long recipientId) {
        List<Notification> unread = notificationRepository
                .findByRecipientIdAndIsReadOrderByCreatedAtDesc(recipientId, false);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void deleteRead(Long recipientId) {
        notificationRepository.deleteByRecipientIdAndIsRead(recipientId, true);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override
    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }
}
