package com.flowboard.user_service.service;

import com.flowboard.user_service.entity.User;
import java.util.List;

public interface UserService {
    User getUserByEmail(String email);
    User getUserById(Long userId);
    User updateProfile(Long userId, User updatedUser);
    String deactivateAccount(Long userId);
    List<User> searchUsers(String name);
}
