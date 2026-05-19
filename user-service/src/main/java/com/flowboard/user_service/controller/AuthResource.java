package com.flowboard.user_service.controller;

import com.flowboard.user_service.dto.*;
import com.flowboard.user_service.entity.User;
import com.flowboard.user_service.repository.UserRepository;
import com.flowboard.user_service.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * AuthResource (Case Study Section 4.1)
 *
 * Exposes:
 *   POST /auth/register
 *   POST /auth/login
 *   POST /auth/logout
 *   POST /auth/refresh
 *   GET  /auth/validate
 *   POST /auth/sync            (OAuth2 social login sync)
 *   POST /auth/password/{id}   (change password by userId)
 *   POST /auth/change-password (change password by email body)
 */
@RestController
@RequestMapping("/auth")
public class AuthResource {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    // POST /auth/register
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO dto) {
        User user = authService.register(com.flowboard.user_service.mapper.UserMapper.toEntity(dto));
        return ResponseEntity.ok(com.flowboard.user_service.mapper.UserMapper.toDTO(user));
    }

    // POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            String token = authService.login(request.getEmail(), request.getPassword());
            User user = userRepository.findByEmail(request.getEmail()).get();
            LoginResponse response = new LoginResponse(token, user.getUserId(), user.getEmail(), user.getRole());
            return ResponseEntity.ok(new ApiResponse<>("Login successful", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(new ApiResponse<>(e.getMessage(), null));
        }
    }

    // POST /auth/logout
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestParam String email) {
        authService.logout(email);
        return ResponseEntity.ok("Logged out successfully");
    }

    // POST /auth/refresh — Case Study: refreshToken()
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            String newToken = authService.refreshToken(token);
            return ResponseEntity.ok(Map.of("token", newToken));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /auth/validate — Case Study: validateToken()
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String jwt = token;
        if (jwt == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        }
        boolean valid = jwt != null && authService.validateToken(jwt);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    // POST /auth/sync — OAuth2 social login
    @PostMapping("/sync")
    public ResponseEntity<UserResponseDTO> syncSocialUser(
            @RequestParam String email,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String avatarUrl,
            @RequestParam(required = false, defaultValue = "SOCIAL") String provider) {
        User user = authService.syncSocialUser(email, fullName, avatarUrl, provider);
        return ResponseEntity.ok(com.flowboard.user_service.mapper.UserMapper.toDTO(user));
    }

    // POST /auth/password/{id} — change password by userId
    @PostMapping("/password/{id}")
    public ResponseEntity<String> changePassword(@PathVariable Long id,
                                                 @RequestParam String oldPassword,
                                                 @RequestParam String newPassword) {
        try {
            return ResponseEntity.ok(authService.changePassword(id, oldPassword, newPassword));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // POST /auth/change-password — change password by email (used by frontend profile page)
    @PostMapping("/change-password")
    public ResponseEntity<?> changePasswordByEmail(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-user-id", required = false) Long userIdFromJwt) {
        try {
            String currentPassword = body.get("currentPassword");
            String newPassword     = body.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }

            // Prefer userId from JWT header (injected by gateway) — most reliable
            User user;
            if (userIdFromJwt != null) {
                user = userRepository.findById(userIdFromJwt)
                        .orElseThrow(() -> new RuntimeException("User not found"));
            } else {
                String email = body.get("email");
                if (email == null) return ResponseEntity.badRequest().body("Missing email");
                user = userRepository.findByEmail(email.trim())
                        .orElseThrow(() -> new RuntimeException("User not found"));
            }

            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                return ResponseEntity.badRequest()
                        .body("This account uses Social Login. Password cannot be changed here.");
            }

            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

            if (!encoder.matches(currentPassword, user.getPasswordHash())) {
                return ResponseEntity.badRequest().body("Wrong password");
            }

            user.setPasswordHash(encoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok("Password updated successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}