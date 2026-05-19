package com.flowboard.notification_service.listener;

import com.flowboard.notification_service.config.RabbitMQConfig;
import com.flowboard.notification_service.dto.NotificationEvent;
import com.flowboard.notification_service.entity.Notification;
import com.flowboard.notification_service.entity.NotificationType;
import com.flowboard.notification_service.service.EmailService;
import com.flowboard.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * NotificationEventListener — Case Study Section 4.8
 *
 * Listens for events on the notification.queue (published by board-service,
 * card-service, workspace-service, etc.) and:
 *   1. Persists a Notification record to the DB.
 *   2. Pushes it via STOMP WebSocket to connected clients.
 *   3. For CRITICAL events (ASSIGNMENT, DUE_DATE): triggers an SMTP email.
 */
import com.flowboard.notification_service.client.UserServiceClient;

@Component
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private com.flowboard.notification_service.repository.NotificationRepository notificationRepository;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotificationEvent(NotificationEvent event) {
        logger.info("Received notification event: type={}, recipient={}, relatedId={}",
                event.getType(), event.getRecipientId(), event.getRelatedId());

        try {
            // Map event type string to enum
            NotificationType type;
            try {
                type = NotificationType.valueOf(event.getType());
            } catch (IllegalArgumentException e) {
                type = NotificationType.MENTION; // fallback
            }

            // ── Deduplication guard ─────────────────────────────────────────
            // Prevent duplicate notifications (e.g. caused by service restarts)
            if (event.getRelatedId() != null && event.getMessage() != null &&
                    notificationRepository.existsByRecipientIdAndMessageAndRelatedId(
                            event.getRecipientId(), event.getMessage(), event.getRelatedId())) {
                logger.warn("[DEDUP] Duplicate notification skipped for recipient={}", event.getRecipientId());
                return;
            }

            // Build and persist Notification
            Notification notification = new Notification();
            notification.setRecipientId(event.getRecipientId());
            notification.setActorId(event.getActorId());
            notification.setType(type);
            notification.setTitle(event.getTitle());
            notification.setMessage(event.getMessage());
            notification.setRelatedId(event.getRelatedId());
            notification.setRelatedType(event.getRelatedType());
            notification.setRead(false);

            // Save to DB + push WebSocket
            notificationService.send(notification);
            logger.info("Notification saved and pushed for recipient={}", event.getRecipientId());

            // ── Critical Event Email Alerts ──────────────────────────────────
            if (type == NotificationType.ASSIGNMENT || type == NotificationType.DUE_DATE
                    || type == NotificationType.CARD_ASSIGNED || type == NotificationType.BOARD_INVITE) {
                sendEmailForEvent(event, type);
            }

        } catch (Exception e) {
            logger.error("Failed to process notification event: {}", e.getMessage(), e);
        }
    }

    /**
     * Resolves recipient email and dispatches the appropriate SMTP email.
     * Runs silently — failures do not affect the notification pipeline.
     */
    private void sendEmailForEvent(NotificationEvent event, NotificationType type) {
        new Thread(() -> {
            try {
                // Fetch recipient user info from user-service
                Map<String, Object> userInfo = userServiceClient.getUser(event.getRecipientId());

                if (userInfo == null || userInfo.get("email") == null) {
                    logger.warn("[EMAIL] Could not resolve email for userId={}", event.getRecipientId());
                    return;
                }

                String recipientEmail = userInfo.get("email").toString();
                String taskName = event.getTitle() != null ? event.getTitle() : "a task";

                if (type == NotificationType.ASSIGNMENT || type == NotificationType.CARD_ASSIGNED) {
                    // Resolve actor's name as "assigned by"
                    String assignedBy = "A team member";
                    if (event.getActorId() != null) {
                        try {
                            Map<String, Object> actorInfo = userServiceClient.getUser(event.getActorId());
                            if (actorInfo != null && actorInfo.get("fullName") != null) {
                                assignedBy = actorInfo.get("fullName").toString();
                            }
                        } catch (Exception ignore) {}
                    }
                    emailService.sendTaskAssignmentEmail(recipientEmail, taskName, assignedBy);
                    logger.info("[EMAIL] Assignment email sent to {} for task: {}", recipientEmail, taskName);

                } else if (type == NotificationType.DUE_DATE) {
                    String dueDate = event.getMessage() != null ? event.getMessage() : "soon";
                    emailService.sendDueDateReminderEmail(recipientEmail, taskName, dueDate);
                    logger.info("[EMAIL] Due date reminder email sent to {} for task: {}", recipientEmail, taskName);
                } else if (type == NotificationType.BOARD_INVITE) {
                    String invitedBy = "A team member";
                    if (event.getActorId() != null) {
                        try {
                            Map<String, Object> actorInfo = userServiceClient.getUser(event.getActorId());
                            if (actorInfo != null && actorInfo.get("fullName") != null) {
                                invitedBy = actorInfo.get("fullName").toString();
                            }
                        } catch (Exception ignore) {}
                    }
                    String boardName = event.getMessage() != null && event.getMessage().contains("\"") ? 
                            event.getMessage().split("\"")[1] : "a board";
                    emailService.sendBoardInvitationEmail(recipientEmail, boardName, invitedBy);
                    logger.info("[EMAIL] Board invitation email sent to {}", recipientEmail);
                }

            } catch (Exception e) {
                logger.warn("[EMAIL] Failed to send critical event email for recipient={}: {}",
                        event.getRecipientId(), e.getMessage());
            }
        }).start();
    }
}
