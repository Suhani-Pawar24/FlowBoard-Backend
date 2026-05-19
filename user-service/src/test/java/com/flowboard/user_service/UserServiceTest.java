package com.flowboard.user_service;

import com.flowboard.user_service.entity.User;
import com.flowboard.user_service.repository.UserRepository;
import com.flowboard.user_service.security.JwtUtil;
import com.flowboard.user_service.service.impl.AuthServiceImpl;
import com.flowboard.user_service.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JUnit Tests for user-service
 *
 * Covers (Case Study Section 4.1):
 *   - User registration
 *   - Login (valid credentials, wrong password, deactivated account)
 *   - Change password
 *   - Profile update
 *   - Search users
 *   - Deactivate account
 *   - Token validation and refresh
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    // ─── Auth Service Tests ───────────────────────────────────────────────────

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setFullName("John Doe");
        testUser.setEmail("john@flowboard.com");
        testUser.setUsername("johndoe");
        testUser.setPasswordHash(encoder.encode("password123"));
        testUser.setRole("USER");
        testUser.setActive(true);
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Register: new user should be saved with hashed password")
    void testRegister_Success() {
        User input = new User();
        input.setFullName("Jane Doe");
        input.setEmail("jane@flowboard.com");
        input.setPassword("secure123");

        when(userRepository.findByEmail("jane@flowboard.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(2L);
            return u;
        });

        User result = authService.register(input);

        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getRole()).isEqualTo("USER");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getPassword()).isNull(); // password field cleared after hashing
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Register: duplicate email should throw RuntimeException")
    void testRegister_DuplicateEmail() {
        User input = new User();
        input.setEmail("john@flowboard.com");
        input.setPassword("pass");

        when(userRepository.findByEmail("john@flowboard.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.register(input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("Register: admin role should be assigned when role=ADMIN provided")
    void testRegister_AdminRole() {
        User input = new User();
        input.setFullName("Admin User");
        input.setEmail("admin@flowboard.com");
        input.setPassword("adminpass");
        input.setRole("ADMIN");

        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.register(input);
        assertThat(result.getRole()).isEqualTo("ADMIN");
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login: valid credentials should return JWT token")
    void testLogin_Success() {
        when(userRepository.findByEmail("john@flowboard.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("mock.jwt.token");

        String token = authService.login("john@flowboard.com", "password123");

        assertThat(token).isEqualTo("mock.jwt.token");
        verify(jwtUtil).generateToken("john@flowboard.com", "USER", 1L);
    }

    @Test
    @DisplayName("Login: wrong password should throw RuntimeException")
    void testLogin_WrongPassword() {
        when(userRepository.findByEmail("john@flowboard.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login("john@flowboard.com", "wrongpass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("Login: deactivated account should throw RuntimeException")
    void testLogin_DeactivatedAccount() {
        testUser.setActive(false);
        when(userRepository.findByEmail("john@flowboard.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login("john@flowboard.com", "password123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    @DisplayName("Login: non-existent email should throw RuntimeException")
    void testLogin_UserNotFound() {
        when(userRepository.findByEmail("nobody@flowboard.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@flowboard.com", "pass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── CHANGE PASSWORD ───────────────────────────────────────────────────────

    @Test
    @DisplayName("ChangePassword: correct old password should update successfully")
    void testChangePassword_Success() {
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = authService.changePassword(1L, "password123", "newpass456");

        assertThat(result).isEqualTo("Password updated");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("ChangePassword: wrong old password should throw exception")
    void testChangePassword_WrongOldPassword() {
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.changePassword(1L, "wrongold", "newpass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Wrong password");
    }

    // ── VALIDATE / REFRESH TOKEN ──────────────────────────────────────────────

    @Test
    @DisplayName("ValidateToken: valid token should return true")
    void testValidateToken_Valid() {
        when(jwtUtil.validateToken("valid.token")).thenReturn(true);
        assertThat(authService.validateToken("valid.token")).isTrue();
    }

    @Test
    @DisplayName("ValidateToken: invalid token should return false")
    void testValidateToken_Invalid() {
        when(jwtUtil.validateToken("bad.token")).thenReturn(false);
        assertThat(authService.validateToken("bad.token")).isFalse();
    }

    @Test
    @DisplayName("RefreshToken: valid token should return new token")
    void testRefreshToken_Success() {
        when(jwtUtil.validateToken("old.token")).thenReturn(true);
        when(jwtUtil.extractEmail("old.token")).thenReturn("john@flowboard.com");
        when(userRepository.findByEmail("john@flowboard.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("new.token");

        String newToken = authService.refreshToken("old.token");
        assertThat(newToken).isEqualTo("new.token");
    }

    @Test
    @DisplayName("RefreshToken: invalid token should throw exception")
    void testRefreshToken_InvalidToken() {
        when(jwtUtil.validateToken("expired.token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("expired.token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid or expired token");
    }

    // ── USER SERVICE ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("GetUserById: existing user should be returned")
    void testGetUserById_Found() {
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(testUser));
        User result = userService.getUserById(1L);
        assertThat(result.getEmail()).isEqualTo("john@flowboard.com");
    }

    @Test
    @DisplayName("GetUserById: missing user should throw exception")
    void testGetUserById_NotFound() {
        when(userRepository.findByUserId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("UpdateProfile: should update name, username, avatarUrl")
    void testUpdateProfile_Success() {
        User updates = new User();
        updates.setFullName("John Updated");
        updates.setUsername("john_updated");
        updates.setAvatarUrl("https://cdn.flowboard.com/avatar.png");

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateProfile(1L, updates);

        assertThat(result.getFullName()).isEqualTo("John Updated");
        assertThat(result.getUsername()).isEqualTo("john_updated");
        assertThat(result.getAvatarUrl()).isEqualTo("https://cdn.flowboard.com/avatar.png");
    }

    @Test
    @DisplayName("DeactivateAccount: should set active=false")
    void testDeactivateAccount() {
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = userService.deactivateAccount(1L);

        assertThat(result).isEqualTo("Account deactivated");
        assertThat(testUser.isActive()).isFalse();
    }

    @Test
    @DisplayName("SearchUsers: should return users matching name")
    void testSearchUsers() {
        when(userRepository.findByFullNameContainingIgnoreCase("john"))
                .thenReturn(List.of(testUser));

        List<User> results = userService.searchUsers("john");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFullName()).isEqualTo("John Doe");
    }
}

