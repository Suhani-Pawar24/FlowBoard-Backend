package com.flowboard.user_service.controller;

import com.flowboard.user_service.entity.AuditLog;
import com.flowboard.user_service.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AuditLogResource — Case Study Section 4.4 (Audit Logs)
 * GET /admin/audit-logs         — paginated audit log list
 * GET /admin/audit-logs/export  — CSV export
 */
@RestController
@RequestMapping("/admin/audit-logs")
public class AuditLogResource {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private void requireAdmin(String role) {
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Access Denied: Platform Admin role required");
        }
    }

    @GetMapping
    public ResponseEntity<?> getAuditLogs(
            @RequestHeader(value = "X-user-role", required = false) String role,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long performedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLog> logs = auditLogRepository.findFiltered(
            (action != null && !action.isBlank()) ? action : null,
            performedBy,
            start,
            end,
            pageable
        );
        return ResponseEntity.ok(Map.of(
            "content", logs.getContent(),
            "totalPages", logs.getTotalPages(),
            "totalElements", logs.getTotalElements(),
            "page", page
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportCsv(
            @RequestHeader(value = "X-user-role", required = false) String role) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }

        List<AuditLog> all = auditLogRepository.findTop100ByOrderByTimestampDesc();
        StringBuilder csv = new StringBuilder("ID,Action,PerformedBy,PerformedByEmail,TargetEntity,TargetId,TargetName,Timestamp,Metadata\n");
        for (AuditLog log : all) {
            csv.append(log.getId()).append(",")
               .append(log.getAction()).append(",")
               .append(log.getPerformedBy()).append(",")
               .append(log.getPerformedByEmail() != null ? log.getPerformedByEmail() : "").append(",")
               .append(log.getTargetEntity() != null ? log.getTargetEntity() : "").append(",")
               .append(log.getTargetId() != null ? log.getTargetId() : "").append(",")
               .append(log.getTargetName() != null ? log.getTargetName().replace(",", ";") : "").append(",")
               .append(log.getTimestamp()).append(",")
               .append(log.getMetadata() != null ? log.getMetadata().replace(",", ";") : "")
               .append("\n");
        }
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=audit-logs.csv")
            .body(csv.toString());
    }
}
