package com.flowboard.user_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AuditLog — Case Study Section 2.4
 * Tracks all significant platform actions for the admin audit trail.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_performed_by", columnList = "performed_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String action; // e.g. USER_SUSPENDED, USER_DELETED, ROLE_CHANGED, BROADCAST_SENT

    @Column(name = "performed_by")
    private Long performedBy; // Admin userId who performed the action

    @Column(name = "performed_by_email", length = 255)
    private String performedByEmail;

    @Column(name = "target_entity", length = 100)
    private String targetEntity; // e.g. USER, WORKSPACE, BOARD, CARD

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_name", length = 500)
    private String targetName; // Human-readable name of the target

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON string with extra context

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
