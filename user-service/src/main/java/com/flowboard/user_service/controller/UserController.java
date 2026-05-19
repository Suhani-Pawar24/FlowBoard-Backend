package com.flowboard.user_service.controller;

import com.flowboard.user_service.dto.UpdateProfileDTO;
import com.flowboard.user_service.dto.UserResponseDTO;
import com.flowboard.user_service.entity.User;
import com.flowboard.user_service.mapper.UserMapper;
import com.flowboard.user_service.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserController — Case Study Section 4.1
 *
 * Exposes:
 *   GET  /user/profile           — get profile of the authenticated user (via X-user-email header)
 *   PUT  /user/profile/{id}      — update profile (fullName, username, avatarUrl)
 *   GET  /user/search?name=...   — search users by full name
 *   DELETE /user/deactivate/{id} — soft-delete (deactivate) account
 *   GET  /user/admin             — admin-only health check
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * GET /user/profile
     * Returns profile of the currently authenticated user.
     * Requires X-user-email header injected by API Gateway after JWT validation.
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @Parameter(hidden = true) @RequestHeader(value = "X-user-email", required = false) String email,
            @Parameter(hidden = true) @RequestHeader(value = "X-user-role",  required = false) String role) {

        if (email == null || role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized: Missing user headers. Please access via API Gateway.");
        }

        logger.info("GET /user/profile for email={} role={}", email, role);

        try {
            UserResponseDTO user = UserMapper.toDTO(userService.getUserByEmail(email));
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            logger.warn("User not found for email={}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    /**
     * GET /user/email
     * Internal endpoint used by API Gateway OAuth2SuccessHandler
     */
    @GetMapping("/email")
    public ResponseEntity<?> getUserByEmail(@RequestParam String email) {
        try {
            UserResponseDTO user = UserMapper.toDTO(userService.getUserByEmail(email));
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
    }

    /**
     * PUT /user/profile/{id}
     * Updates fullName, username, and/or avatarUrl for the given userId.
     * Uses UpdateProfileDTO (no password required).
     */
    @PutMapping("/profile/{id}")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileDTO dto) {

        logger.info("PUT /user/profile/{}", id);

        // Map UpdateProfileDTO → User entity (only profile fields)
        User partial = new User();
        partial.setFullName(dto.getFullName());
        partial.setUsername(dto.getUsername());
        partial.setAvatarUrl(dto.getAvatarUrl());

        User updated = userService.updateProfile(id, partial);
        return ResponseEntity.ok(UserMapper.toDTO(updated));
    }

    /**
     * GET /user/{id}
     * Returns profile of any user by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        logger.info("GET /user/{}", id);
        return ResponseEntity.ok(UserMapper.toDTO(userService.getUserById(id)));
    }

    /**
     * GET /user/search?name=...
     * Returns users whose full name contains the query string (case-insensitive).
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserResponseDTO>> search(@RequestParam String name) {
        logger.info("GET /user/search?name={}", name);
        List<UserResponseDTO> results = userService.searchUsers(name)
                .stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    /**
     * DELETE /user/deactivate/{id}
     * Soft-deletes the account by setting active=false.
     * Account can be reactivated by a Platform Admin.
     */
    @DeleteMapping("/deactivate/{id}")
    public ResponseEntity<String> deactivate(@PathVariable Long id) {
        logger.info("DELETE /user/deactivate/{}", id);
        String result = userService.deactivateAccount(id);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /user/admin
     * Admin-role health check — used to verify role-based access through the Gateway.
     */
    @GetMapping("/admin")
    public ResponseEntity<?> adminOnly(
            @Parameter(hidden = true) @RequestHeader(value = "X-user-role", required = false) String role) {

        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized: Missing role header. Please access via API Gateway.");
        }
        logger.info("GET /user/admin — role={}", role);

        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Admin role required");
        }

        return ResponseEntity.ok("Welcome Admin! You have access to this protected resource.");
    }
}
