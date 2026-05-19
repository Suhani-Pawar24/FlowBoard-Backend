package com.flowboard.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * User Entity — Case Study Section 4.1
 *
 * Stores: userId, fullName, email, passwordHash, username, role,
 *         avatarUrl, provider (OAuth), isActive, createdAt, updatedAt
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "username")
    }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Transient plain-text password — only used during registration/login.
     * Never persisted to the database. Excluded from JSON serialization.
     */
    @Transient
    @JsonIgnore
    private String password;

    /** BCrypt-hashed password — stored in DB, never exposed in responses. */
    @JsonIgnore
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "username", unique = true)
    private String username;

    /**
     * Role string — values: USER, ADMIN
     * Case Study maps to: MEMBER, BOARD_OWNER, PLATFORM_ADMIN
     */
    @Column(nullable = false)
    private String role;

    @Column(name = "avatar_url")
    private String avatarUrl;

    /** OAuth2 provider identifier, e.g. "GOOGLE", or null for local auth */
    private String provider;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}