package com.flowboard.user_service.controller;

import com.flowboard.user_service.entity.AuditLog;
import com.flowboard.user_service.entity.User;
import com.flowboard.user_service.dto.UserResponseDTO;
import com.flowboard.user_service.mapper.UserMapper;
import com.flowboard.user_service.repository.AuditLogRepository;
import com.flowboard.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extended UserAdminResource with:
 *  - Role change: PATCH /admin/users/{id}/role?newRole=ADMIN
 *  - All mutating actions now produce AuditLog records
 */
@RestController
@RequestMapping("/admin/users/actions")
public class UserAdminActionsResource {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private void requireAdmin(String role) {
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Access Denied: Platform Admin role required");
        }
    }

    private void log(String action, Long adminId, String adminEmail, String targetEntity, Long targetId, String targetName) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setPerformedBy(adminId);
        log.setPerformedByEmail(adminEmail);
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        auditLogRepository.save(log);
    }

    /**
     * PATCH /admin/users/actions/{userId}/role?newRole=ADMIN
     * Changes the role of a user (USER ↔ ADMIN).
     */
    @PutMapping("/{userId}/role")
    public ResponseEntity<?> changeRole(
            @PathVariable Long userId,
            @RequestParam String newRole,
            @RequestHeader(value = "X-user-role", required = false) String role,
            @RequestHeader(value = "X-user-id", required = false) Long adminId,
            @RequestHeader(value = "X-user-email", required = false) String adminEmail) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setRole(newRole.toUpperCase());
        User saved = userRepository.save(user);
        log("ROLE_CHANGED", adminId, adminEmail, "USER", userId, user.getEmail() + " → " + newRole);
        return ResponseEntity.ok(UserMapper.toDTO(saved));
    }

    /**
     * PATCH /admin/users/actions/{userId}/suspend
     */
    @PutMapping("/{userId}/suspend")
    public ResponseEntity<?> suspendUser(
            @PathVariable Long userId,
            @RequestHeader(value = "X-user-role", required = false) String role,
            @RequestHeader(value = "X-user-id", required = false) Long adminId,
            @RequestHeader(value = "X-user-email", required = false) String adminEmail) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        userRepository.save(user);
        log("USER_SUSPENDED", adminId, adminEmail, "USER", userId, user.getEmail());
        return ResponseEntity.ok(Map.of("status", "suspended", "userId", userId));
    }

    /**
     * PATCH /admin/users/actions/{userId}/reactivate
     */
    @PutMapping("/{userId}/reactivate")
    public ResponseEntity<?> reactivateUser(
            @PathVariable Long userId,
            @RequestHeader(value = "X-user-role", required = false) String role,
            @RequestHeader(value = "X-user-id", required = false) Long adminId,
            @RequestHeader(value = "X-user-email", required = false) String adminEmail) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
        userRepository.save(user);
        log("USER_REACTIVATED", adminId, adminEmail, "USER", userId, user.getEmail());
        return ResponseEntity.ok(Map.of("status", "reactivated", "userId", userId));
    }

    /**
     * DELETE /admin/users/actions/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long userId,
            @RequestHeader(value = "X-user-role", required = false) String role,
            @RequestHeader(value = "X-user-id", required = false) Long adminId,
            @RequestHeader(value = "X-user-email", required = false) String adminEmail) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
        User user = userRepository.findById(userId).orElse(null);
        String email = user != null ? user.getEmail() : "unknown";
        userRepository.deleteById(userId);
        log("USER_DELETED", adminId, adminEmail, "USER", userId, email);
        return ResponseEntity.noContent().build();
    }
}
