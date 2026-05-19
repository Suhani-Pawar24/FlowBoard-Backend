package com.flowboard.user_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * UserResponseDTO — Case Study Section 4.1
 *
 * Safe response object: no passwordHash, no transient password.
 * Includes all fields safe for external exposure.
 */
@Data
public class UserResponseDTO {

    private Long userId;
    private String fullName;
    private String email;
    private String username;
    private String role;
    private String avatarUrl;
    private String provider;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
