package com.flowboard.notification_service.controller;

import com.flowboard.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.flowboard.notification_service.dto.EmailNotificationRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications/email")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/test")
    public ResponseEntity<String> sendTestEmail(@RequestBody Map<String, String> payload) {
        String type = payload.get("type");
        String to = payload.get("to");
        
        if (to == null || to.isEmpty()) {
            return ResponseEntity.badRequest().body("Recipient email ('to') is required");
        }

        try {
            if ("ASSIGNMENT".equalsIgnoreCase(type)) {
                emailService.sendTaskAssignmentEmail(to, "Sample Task", "Admin");
            } else if ("REMINDER".equalsIgnoreCase(type)) {
                emailService.sendDueDateReminderEmail(to, "Sample Task", "Tomorrow");
            } else if ("INVITATION".equalsIgnoreCase(type)) {
                emailService.sendWorkspaceInvitationEmail(to, "Sample Workspace", "Admin");
            } else {
                return ResponseEntity.badRequest().body("Invalid type. Use ASSIGNMENT, REMINDER, or INVITATION");
            }
            return ResponseEntity.ok("Test email triggered successfully to " + to);
        } catch (Exception e) {
            log.error("Error triggering test email", e);
            return ResponseEntity.internalServerError().body("Failed to trigger email: " + e.getMessage());
        }
    }
    @PostMapping("/send")
    public ResponseEntity<String> sendEmailNotification(@RequestBody EmailNotificationRequest request) {
        try {
            if ("ASSIGNMENT".equalsIgnoreCase(request.getType())) {
                emailService.sendTaskAssignmentEmail(request.getTo(), request.getItemName(), request.getSenderName());
            } else if ("REMINDER".equalsIgnoreCase(request.getType())) {
                emailService.sendDueDateReminderEmail(request.getTo(), request.getItemName(), request.getDueDate());
            } else if ("INVITATION".equalsIgnoreCase(request.getType())) {
                emailService.sendWorkspaceInvitationEmail(request.getTo(), request.getItemName(), request.getSenderName());
            } else {
                return ResponseEntity.badRequest().body("Invalid notification type");
            }
            return ResponseEntity.ok("Email sent successfully to " + request.getTo());
        } catch (Exception e) {
            log.error("Failed to send email notification to {}", request.getTo(), e);
            return ResponseEntity.internalServerError().body("Failed to send email: " + e.getMessage());
        }
    }
}
