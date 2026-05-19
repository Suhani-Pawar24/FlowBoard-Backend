package com.flowboard.user_service.service;

import com.flowboard.user_service.entity.User;

/**
 * Auth Service Interface (Case Study Section 4.1)
 *
 * Declares: register(), login(), logout(), validateToken(), refreshToken(),
 * getUserByEmail(), getUserById(), updateProfile(), changePassword(),
 * deactivateAccount(), searchUsers()
 *
 * Note: getUserByEmail, getUserById, updateProfile, deactivateAccount, searchUsers
 * are declared in UserService interface and implemented in UserServiceImpl.
 */
public interface AuthService {
    User register(User user);
    String login(String email, String password);
    void logout(String email);
    String changePassword(Long userId, String oldPassword, String newPassword);
    User syncSocialUser(String email, String fullName, String avatarUrl, String provider);
    boolean validateToken(String token);
    String refreshToken(String token);
}
