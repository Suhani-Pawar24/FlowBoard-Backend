package com.flowboard.user_service.controller;

import com.flowboard.user_service.dto.UserResponseDTO;
import com.flowboard.user_service.entity.User;
import com.flowboard.user_service.mapper.UserMapper;
import com.flowboard.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserAdminResource — Case Study Section 2.4 (Platform Administrator)
 *
 * Exposes:
 *   GET    /admin/users             — list all users
 *   GET    /admin/users/{userId}    — get a specific user
 *   PUT    /admin/users/{userId}/suspend   — deactivate a user account
 *   PUT    /admin/users/{userId}/activate  — reactivate a user account
 *   DELETE /admin/users/{userId}    — permanently delete a user
 *
 * Access gated at the API Gateway by X-user-role: ADMIN header.
 */
@RestController
@RequestMapping("/admin/users")
public class UserAdminResource {

    @Autowired
    private UserRepository userRepository;

    /**
     * Validates that the caller has Platform Admin role.
     * Throws RuntimeException if not authorized.
     */
    private void requireAdmin(String role) {
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Access Denied: Platform Admin role required");
        }
    }

    /**
     * GET /admin/users
     * Returns all registered users as safe DTOs (no passwordHash).
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "X-user-role", required = false) String role) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
        List<UserResponseDTO> users = userRepository.findAll()
                .stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * GET /admin/users/{userId}
     * Returns a single user by ID.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId,
            @RequestHeader(value = "X-user-role", required = false) String role) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
        return userRepository.findById(userId)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserMapper.toDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /admin/users/{userId}/suspend
     * Sets active = false; account becomes unusable until reactivated.
     */
    @PutMapping("/{userId}/suspend")
    public ResponseEntity<?> suspendUser(@PathVariable Long userId,
            @RequestHeader(value = "X-user-role", required = false) String role) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setActive(false);
        User saved = userRepository.save(user);
        return ResponseEntity.ok(UserMapper.toDTO(saved));
    }

    /**
     * PUT /admin/users/{userId}/activate
     * Sets active = true; restores the ability to log in.
     */
    @PutMapping("/{userId}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long userId,
            @RequestHeader(value = "X-user-role", required = false) String role) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setActive(true);
        User saved = userRepository.save(user);
        return ResponseEntity.ok(UserMapper.toDTO(saved));
    }

    /**
     * DELETE /admin/users/{userId}
     * Permanently removes the user from the platform. Irreversible.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId,
            @RequestHeader(value = "X-user-role", required = false) String role) {
        try {
            requireAdmin(role);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(userId);
        return ResponseEntity.noContent().build();
    }
}
