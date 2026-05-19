package com.flowboard.user_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Broadcast — persists platform-wide admin notifications to DB.
 */
@Entity
@Table(name = "broadcasts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Broadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "target_role", length = 50)
    private String targetRole; // ALL, USER, ADMIN

    @Column(name = "sent_by")
    private Long sentBy; // Admin userId

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }
}
