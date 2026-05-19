package com.flowboard.user_service.service.impl;

import com.flowboard.user_service.entity.User;
import com.flowboard.user_service.repository.UserRepository;
import com.flowboard.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    @Override
    public User updateProfile(Long userId, User updatedUser) {
        User user = getUserById(userId);

        if (updatedUser.getFullName() != null)
            user.setFullName(updatedUser.getFullName());

        if (updatedUser.getUsername() != null)
            user.setUsername(updatedUser.getUsername());

        if (updatedUser.getRole() != null)
            user.setRole(updatedUser.getRole());

        if (updatedUser.getAvatarUrl() != null)
            user.setAvatarUrl(updatedUser.getAvatarUrl());

        return userRepository.save(user);
    }

    @Override
    public String deactivateAccount(Long userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
        return "Account deactivated";
    }

    @Override
    public List<User> searchUsers(String name) {
        return userRepository.findByFullNameContainingIgnoreCase(name);
    }
}
