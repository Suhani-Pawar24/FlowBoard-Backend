package com.flowboard.user_service.controller;

import com.flowboard.user_service.entity.Broadcast;
import com.flowboard.user_service.entity.AuditLog;
import com.flowboard.user_service.repository.BroadcastRepository;
import com.flowboard.user_service.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * BroadcastAdminResource — replaces old in-memory BroadcastResource.
 * Now persists broadcasts to DB and logs to audit trail.
 *
 * POST /admin/notifications/broadcast
 * GET  /admin/notifications/broadcasts  (paginated history)
 */
import com.flowboard.user_service.client.NotificationServiceClient;

@RestController
@RequestMapping("/admin/notifications")
public class BroadcastAdminResource {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastAdminResource.class);

    @Autowired
    private BroadcastRepository broadcastRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private com.flowboard.user_service.repository.UserRepository userRepository;

    @Autowired
    private NotificationServiceClient notificationServiceClient;

    private void requireAdmin(String role) {
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Access Denied: Platform Admin role required");
        }
    }

    /**
     * POST /admin/notifications/broadcast
     * Body: { "message": "...", "title": "...", "targetRole": "ALL|USER|ADMIN" }
     */
    @PostMapping("/broadcast")
    public ResponseEntity<?> sendBroadcast(
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-user-role", required = false) String role,
            @RequestHeader(value = "X-user-id", required = false) Long adminId,
            @RequestHeader(value = "X-user-email", required = false) String adminEmail) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }

        String message = payload.getOrDefault("message", "");
        String title   = payload.getOrDefault("title", "Platform Announcement");
        String target  = payload.getOrDefault("targetRole", "ALL");

        if (message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Broadcast message cannot be empty"));
        }

        // Persist to DB
        Broadcast broadcast = new Broadcast();
        broadcast.setTitle(title);
        broadcast.setMessage(message);
        broadcast.setTargetRole(target);
        broadcast.setSentBy(adminId);
        broadcast.setSentAt(LocalDateTime.now());
        Broadcast saved = broadcastRepository.save(broadcast);

        // Audit log
        AuditLog log = new AuditLog();
        log.setAction("BROADCAST_SENT");
        log.setPerformedBy(adminId);
        log.setPerformedByEmail(adminEmail);
        log.setTargetEntity("BROADCAST");
        log.setTargetId(saved.getId());
        log.setTargetName(title);
        log.setMetadata("{\"targetRole\":\"" + target + "\",\"message\":\"" + message.substring(0, Math.min(100, message.length())) + "\"}");
        auditLogRepository.save(log);

        logger.info("PLATFORM BROADCAST [{}] → {}: {}", target, title, message);

        // Forward to notification-service (best-effort)
        try {
            java.util.List<com.flowboard.user_service.entity.User> targetUsers;
            if ("ALL".equalsIgnoreCase(target)) {
                targetUsers = userRepository.findAll();
            } else {
                targetUsers = userRepository.findAllByRole(target);
            }

            java.util.List<Map<String, Object>> bulkNotifications = new java.util.ArrayList<>();
            for (com.flowboard.user_service.entity.User u : targetUsers) {
                if (adminId != null && adminId.equals(u.getUserId())) {
                    continue; // Do not send broadcast to the admin who dispatched it
                }
                bulkNotifications.add(Map.of(
                    "recipientId", u.getUserId(),
                    "actorId", adminId != null ? adminId : 0L,
                    "type", "SYSTEM",
                    "title", title,
                    "message", message,
                    "read", false
                ));
            }

            if (!bulkNotifications.isEmpty()) {
                notificationServiceClient.sendBulkNotifications(bulkNotifications);
                logger.info("Sent {} notifications to bulk endpoint via Feign", bulkNotifications.size());
            }
        } catch (Exception e) {
            logger.warn("Could not forward broadcast: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
            "status", "sent",
            "broadcastId", saved.getId(),
            "message", "Broadcast dispatched successfully"
        ));
    }

    /**
     * GET /admin/notifications/broadcasts
     * Returns paginated broadcast history from DB.
     */
    @GetMapping("/broadcasts")
    public ResponseEntity<?> getBroadcastHistory(
            @RequestHeader(value = "X-user-role", required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
        Page<Broadcast> broadcasts = broadcastRepository.findAllByOrderBySentAtDesc(
            PageRequest.of(page, size)
        );
        return ResponseEntity.ok(Map.of(
            "content", broadcasts.getContent(),
            "totalPages", broadcasts.getTotalPages(),
            "totalElements", broadcasts.getTotalElements()
        ));
    }

    /**
     * Legacy endpoint kept for backward compatibility.
     * GET /admin/notifications/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<?> getLatest(
            @RequestHeader(value = "X-user-role", required = false) String role) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
        Page<Broadcast> recent = broadcastRepository.findAllByOrderBySentAtDesc(PageRequest.of(0, 50));
        return ResponseEntity.ok(recent.getContent());
    }
}
