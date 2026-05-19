package com.flowboard.user_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

/**
 * OverdueCardsAdminResource — GET /admin/overdue-cards
 * Proxies to card-service to fetch all cards where dueDate < now and status != DONE.
 * POST /admin/overdue-cards/remind — sends bulk overdue reminders (STOMP + Email)
 */
import com.flowboard.user_service.client.BoardServiceClient;
import com.flowboard.user_service.client.CardServiceClient;
import com.flowboard.user_service.client.NotificationServiceClient;
import com.flowboard.user_service.repository.UserRepository;
import com.flowboard.user_service.entity.User;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/admin/overdue-cards")
public class OverdueCardsAdminResource {

    private static final Logger logger = LoggerFactory.getLogger(OverdueCardsAdminResource.class);

    @Autowired
    private CardServiceClient cardServiceClient;

    @Autowired
    private BoardServiceClient boardServiceClient;

    @Autowired
    private NotificationServiceClient notificationServiceClient;

    @Autowired
    private UserRepository userRepository;

    private void requireAdmin(String role) {
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Access Denied: Platform Admin role required");
        }
    }

    @GetMapping
    public ResponseEntity<?> getOverdueCards(
            @RequestHeader(value = "X-user-role", required = false) String role) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }

        try {
            List<Map<String, Object>> overdueCards = cardServiceClient.getOverdueCards();
            if (overdueCards == null || overdueCards.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            // Fetch active boards from board-service to filter out orphaned cards
            try {
                List<Map<String, Object>> boards = boardServiceClient.getAllBoards("ADMIN");
                if (boards != null) {
                    java.util.Set<Long> validBoardIds = new java.util.HashSet<>();
                    for (Map<String, Object> b : boards) {
                        if (b.get("boardId") != null) {
                            validBoardIds.add(((Number) b.get("boardId")).longValue());
                        }
                    }
                    overdueCards = overdueCards.stream()
                        .filter(c -> c.get("boardId") != null && validBoardIds.contains(((Number) c.get("boardId")).longValue()))
                        .toList();
                }
            } catch (Exception ex) {
                logger.warn("Failed to fetch boards for filtering: {}", ex.getMessage());
            }

            return ResponseEntity.ok(overdueCards);
        } catch (Exception e) {
            logger.warn("card-service unavailable for overdue cards: {}", e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * GET /admin/overdue-cards/summary
     * Returns count of overdue cards (lightweight for dashboard widget).
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getOverdueSummary(
            @RequestHeader(value = "X-user-role", required = false) String role) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }

        try {
            List<Map<String, Object>> overdueCards = cardServiceClient.getOverdueCards();
            int count = overdueCards != null ? overdueCards.size() : 0;
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
    }

    /**
     * POST /admin/overdue-cards/remind
     * Fetches all overdue cards that have an assignee, then sends:
     *  1. A DUE_DATE bulk notification to the notification-service (→ STOMP WebSocket)
     *  2. An email reminder via the notification-service email endpoint
     */
    @PostMapping("/remind")
    public ResponseEntity<?> sendOverdueReminders(
            @RequestHeader(value = "X-user-role", required = false) String role,
            @RequestHeader(value = "X-user-id", required = false) Long adminId) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }

        try {
            // Fetch overdue cards from card-service
            List<Map<String, Object>> overdueCards;
            try {
                overdueCards = cardServiceClient.getOverdueCards();
            } catch (Exception e) {
                logger.error("Failed to fetch overdue cards: {}", e.getMessage());
                return ResponseEntity.status(502).body(Map.of("error", "card-service unavailable: " + e.getMessage()));
            }

            if (overdueCards == null || overdueCards.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "no_overdue", "count", 0,
                        "message", "No overdue cards found. All tasks are on track!"));
            }

            // Fetch active boards from board-service to filter out orphaned cards
            try {
                List<Map<String, Object>> boards = boardServiceClient.getAllBoards("ADMIN");
                if (boards != null) {
                    java.util.Set<Long> validBoardIds = new java.util.HashSet<>();
                    for (Map<String, Object> b : boards) {
                        if (b.get("boardId") != null) {
                            validBoardIds.add(((Number) b.get("boardId")).longValue());
                        }
                    }
                    overdueCards = overdueCards.stream()
                        .filter(c -> c.get("boardId") != null && validBoardIds.contains(((Number) c.get("boardId")).longValue()))
                        .toList();
                }
            } catch (Exception ex) {
                logger.warn("Failed to fetch boards for filtering: {}", ex.getMessage());
            }

            if (overdueCards.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "no_overdue", "count", 0,
                        "message", "No valid overdue cards found."));
            }

            // Filter cards that have an assignee
            List<Map<String, Object>> assignedOverdue = new ArrayList<>();
            for (Map<String, Object> card : overdueCards) {
                if (card.get("assigneeId") != null) {
                    assignedOverdue.add(card);
                }
            }

            if (assignedOverdue.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "no_assignees", "count", 0,
                        "message", "Overdue cards exist but none have assignees."));
            }

            // Build bulk notification payload for STOMP (one per assignee)
            List<Map<String, Object>> bulkNotifications = new ArrayList<>();
            int emailsSent = 0;
            int notifsSent = 0;

            for (Map<String, Object> card : assignedOverdue) {
                Object assigneeIdObj = card.get("assigneeId");
                if (assigneeIdObj == null) continue;

                long assigneeId = ((Number) assigneeIdObj).longValue();
                String cardTitle = card.getOrDefault("title", "Unknown Task").toString();
                String dueDate = card.get("dueDate") != null ? card.get("dueDate").toString() : "overdue";

                // 1. Add to bulk STOMP notification
                Map<String, Object> notif = new LinkedHashMap<>();
                notif.put("recipientId", assigneeId);
                notif.put("actorId", adminId != null ? adminId : 0L);
                notif.put("type", "DUE_DATE");
                notif.put("title", "⚠️ Overdue Reminder: " + cardTitle);
                notif.put("message", "Your task \"" + cardTitle + "\" is overdue (due: "
                        + dueDate.substring(0, Math.min(10, dueDate.length())) + "). Please action it immediately.");
                notif.put("read", false);
                bulkNotifications.add(notif);
                notifsSent++;

                // 2. Send email reminder via notification-service
                try {
                    // Resolve assignee email from user-service
                    User user = userRepository.findById(assigneeId).orElse(null);

                    if (user != null && user.getEmail() != null) {
                        String email = user.getEmail();
                        String dueDateShort = dueDate.substring(0, Math.min(10, dueDate.length()));

                        Map<String, String> emailPayload = new LinkedHashMap<>();
                        emailPayload.put("type", "REMINDER");
                        emailPayload.put("to", email);
                        emailPayload.put("itemName", cardTitle);
                        emailPayload.put("dueDate", dueDateShort);
                        emailPayload.put("senderName", "FlowBoard Admin");

                        notificationServiceClient.sendEmail(emailPayload);
                        emailsSent++;
                        logger.info("[REMINDER EMAIL] Sent to {} for card: {}", email, cardTitle);
                    }
                } catch (Exception emailEx) {
                    logger.warn("[REMINDER EMAIL] Failed for assignee {}: {}", assigneeId, emailEx.getMessage());
                }
            }

            // 3. Dispatch all STOMP notifications in bulk
            if (!bulkNotifications.isEmpty()) {
                try {
                    notificationServiceClient.sendBulkNotifications(bulkNotifications);
                    logger.info("[OVERDUE REMIND] Sent {} bulk STOMP notifications", bulkNotifications.size());
                } catch (Exception bulkEx) {
                    logger.warn("[OVERDUE REMIND] Bulk STOMP dispatch failed: {}", bulkEx.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "status", "sent",
                    "overdueCards", assignedOverdue.size(),
                    "notificationsSent", notifsSent,
                    "emailsSent", emailsSent,
                    "message", "Reminders dispatched to " + notifsSent + " assignees (" + emailsSent + " emails sent)"
            ));

        } catch (Exception e) {
            logger.error("[OVERDUE REMIND] Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
