package com.flowboard.user_service.repository;

import com.flowboard.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUserId(Long userId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByRole(String role);

    List<User> findByFullNameContainingIgnoreCase(String fullName);

    void deleteByUserId(Long userId);
}