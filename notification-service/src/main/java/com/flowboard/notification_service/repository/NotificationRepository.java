package com.flowboard.notification_service.repository;

import com.flowboard.notification_service.entity.Notification;
import com.flowboard.notification_service.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * NotificationRepository — Case Study Section 4.8
 *
 * findByRecipientId(), findByRecipientIdAndIsRead(),
 * countByRecipientIdAndIsRead(), findByType(),
 * findByRelatedId(), deleteByNotificationId(),
 * deleteByRecipientIdAndIsRead()
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    List<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(Long recipientId, boolean isRead);
    long countByRecipientIdAndIsRead(Long recipientId, boolean isRead);
    List<Notification> findByType(NotificationType type);
    List<Notification> findByRelatedId(Long relatedId);
    void deleteByNotificationId(Long notificationId);
    void deleteByRecipientIdAndIsRead(Long recipientId, boolean isRead);

    // Deduplication check — prevent same message for same recipient+board being stored twice
    boolean existsByRecipientIdAndMessageAndRelatedId(Long recipientId, String message, Long relatedId);
}
