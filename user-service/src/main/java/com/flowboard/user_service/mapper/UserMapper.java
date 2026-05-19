package com.flowboard.user_service.mapper;

import com.flowboard.user_service.dto.UserRequestDTO;
import com.flowboard.user_service.dto.UserResponseDTO;
import com.flowboard.user_service.entity.User;

/**
 * UserMapper — bidirectional mapping between User entity and DTOs.
 */
public class UserMapper {

    // RequestDTO → Entity
    public static User toEntity(UserRequestDTO dto) {
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setUsername(dto.getUsername());
        user.setRole(dto.getRole());
        user.setAvatarUrl(dto.getAvatarUrl());
        return user;
    }

    // Entity → ResponseDTO
    public static UserResponseDTO toDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setProvider(user.getProvider());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
