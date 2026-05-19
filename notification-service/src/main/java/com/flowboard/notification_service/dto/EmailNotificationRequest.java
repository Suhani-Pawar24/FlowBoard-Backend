package com.flowboard.notification_service.dto;

import lombok.Data;

@Data
public class EmailNotificationRequest {
    private String type;          // ASSIGNMENT, INVITATION, REMINDER
    private String to;            // recipient email address
    private String recipientName; // recipient's display name
    private String itemName;      // task name or workspace name
    private String senderName;    // "assigned by" or "invited by" person
    private String dueDate;       // only for REMINDER type
}
