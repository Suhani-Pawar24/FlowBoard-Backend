package com.flowboard.user_service.service.impl;

import com.flowboard.user_service.entity.User;
import com.flowboard.user_service.repository.UserRepository;
import com.flowboard.user_service.security.JwtUtil;
import com.flowboard.user_service.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthServiceImpl (Case Study Section 4.1)
 *
 * Implements: register, login, logout, JWT generation and validation,
 * OAuth2 handling, profile update, password change, account deactivation, user search.
 *
 * Note: profile update, deactivation, and search are in UserServiceImpl.
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public User register(User user) {
        return userRepository.findByEmail(user.getEmail()).map(existingUser -> {
            // If user exists but has no password (OAuth user), allow "upgrading" to email/pass login
            if (existingUser.getPasswordHash() == null || existingUser.getPasswordHash().isBlank()) {
                existingUser.setPasswordHash(encoder.encode(user.getPassword()));
                // If they provide a full name during registration, update it
                if (user.getFullName() != null) existingUser.setFullName(user.getFullName());
                return userRepository.save(existingUser);
            }
            throw new RuntimeException("Email already exists");
        }).orElseGet(() -> {
            if (user.getUsername() == null) {
                user.setUsername(user.getEmail().split("@")[0]);
            }

            user.setPasswordHash(encoder.encode(user.getPassword()));
            user.setPassword(null);

            // Dynamic role assignment
            String roleStr = (user.getRole() != null) ? user.getRole().toUpperCase() : "USER";
            user.setRole(roleStr);
            user.setActive(true);

            return userRepository.save(user);
        });
    }

    @Override
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        // If the account was created via Social login and has no password set
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new RuntimeException("This account uses Social Login. Please sign in with Google.");
        }

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getUserId());
    }

    @Override
    public void logout(String email) {
        // In a stateless JWT system, logout is handled client-side by discarding the token.
        // For a more robust solution, a token blacklist (Redis) would be used.
    }

    @Override
    public String changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Wrong password");
        }

        user.setPasswordHash(encoder.encode(newPassword));
        userRepository.save(user);

        return "Password updated";
    }

    @Override
    public User syncSocialUser(String email, String fullName, String avatarUrl, String provider) {
        return userRepository.findByEmail(email).map(user -> {
            if (fullName != null && (user.getFullName() == null || user.getFullName().isBlank())) {
                user.setFullName(fullName);
            }
            if (avatarUrl != null && (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())) {
                user.setAvatarUrl(avatarUrl);
            }
            if (provider != null && (user.getProvider() == null || "SOCIAL".equals(user.getProvider()))) {
                user.setProvider(provider.toUpperCase());
            }
            // Ensure they are active if syncing
            user.setActive(true);
            return userRepository.save(user);
        }).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(fullName);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setUsername(email.split("@")[0]);
            newUser.setRole("USER");
            newUser.setActive(true);
            newUser.setProvider(provider != null ? provider.toUpperCase() : "SOCIAL");
            return userRepository.save(newUser);
        });
    }

    /**
     * Case Study: validateToken() — validates a JWT token's signature and expiry.
     */
    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    /**
     * Case Study: refreshToken() — issues a new JWT token from a valid existing token.
     * The old token must still be valid (not expired).
     */
    @Override
    public String refreshToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }
        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getUserId());
    }
}