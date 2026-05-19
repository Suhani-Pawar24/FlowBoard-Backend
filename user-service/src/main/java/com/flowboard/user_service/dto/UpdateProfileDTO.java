package com.flowboard.user_service.dto;

import lombok.Data;

/**
 * UpdateProfileDTO — used for PUT /user/profile/{id}
 *
 * Separate from UserRequestDTO so that profile updates do NOT require
 * a password field (which would fail @NotBlank validation on UserRequestDTO).
 */
@Data
public class UpdateProfileDTO {

    private String fullName;
    private String username;
    private String avatarUrl;
}
