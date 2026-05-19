package com.flowboard.user_service.controller;

import com.flowboard.user_service.entity.AuditLog;
import com.flowboard.user_service.repository.AuditLogRepository;
import com.flowboard.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AdminAnalyticsResource — GET /admin/analytics
 * Returns platform-wide statistics for the admin dashboard.
 */
import com.flowboard.user_service.client.WorkspaceServiceClient;
import com.flowboard.user_service.client.BoardServiceClient;
import com.flowboard.user_service.client.CardServiceClient;

@RestController
@RequestMapping("/admin/analytics")
public class AdminAnalyticsResource {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private WorkspaceServiceClient workspaceServiceClient;

    @Autowired
    private BoardServiceClient boardServiceClient;

    @Autowired
    private CardServiceClient cardServiceClient;

    private void requireAdmin(String role) {
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Access Denied: Platform Admin role required");
        }
    }

    @GetMapping
    public ResponseEntity<?> getPlatformStats(
            @RequestHeader(value = "X-user-role", required = false) String role) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }

        Map<String, Object> stats = new HashMap<>();

        // User stats from local DB
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream().filter(u -> u.isActive()).count();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);

        // Workspace count from workspace-service
        try {
            List workspaces = workspaceServiceClient.getWorkspaces();
            stats.put("totalWorkspaces", workspaces != null ? workspaces.size() : 0);
        } catch (Exception e) {
            stats.put("totalWorkspaces", 0);
        }

        // Board count from board-service
        try {
            List boards = boardServiceClient.getBoards();
            stats.put("totalBoards", boards != null ? boards.size() : 0);
        } catch (Exception e) {
            stats.put("totalBoards", 0);
        }

        // Overdue cards from card-service
        try {
            List overdueCards = cardServiceClient.getOverdueCards();
            stats.put("overdueCards", overdueCards != null ? overdueCards.size() : 0);
        } catch (Exception e) {
            stats.put("overdueCards", 0);
        }

        // Audit log activity for last 7 days
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            List<Object[]> dayActivity = auditLogRepository.countByDay(since);
            List<Map<String, Object>> activity = new ArrayList<>();
            for (Object[] row : dayActivity) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("date", row[0] != null ? row[0].toString() : "");
                entry.put("count", row[1]);
                activity.add(entry);
            }
            stats.put("activityLast7Days", activity);
        } catch (Exception e) {
            stats.put("activityLast7Days", new ArrayList<>());
        }

        // Recent activity feed (last 10 audit logs)
        try {
            List<AuditLog> recent = auditLogRepository.findTop100ByOrderByTimestampDesc();
            stats.put("recentActivity", recent.subList(0, Math.min(10, recent.size())));
        } catch (Exception e) {
            stats.put("recentActivity", new ArrayList<>());
        }

        return ResponseEntity.ok(stats);
    }
}
