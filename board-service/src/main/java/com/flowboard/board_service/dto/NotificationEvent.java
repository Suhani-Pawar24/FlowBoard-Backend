package com.flowboard.board_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NotificationEvent DTO — Published to RabbitMQ by board-service.
 * Consumed by notification-service to create a Notification record
 * and push a real-time update via STOMP WebSocket.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private Long recipientId;   // User who receives the notification
    private Long actorId;       // User who triggered the action
    private String type;        // e.g., BOARD_INVITE, CARD_ASSIGNED, COMMENT_ADDED
    private String title;       // Short title
    private String message;     // Full message body
    private Long relatedId;     // Board/Card/Workspace ID
    private String relatedType; // "BOARD", "CARD", "WORKSPACE"
}
