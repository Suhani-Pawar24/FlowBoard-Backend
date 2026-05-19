package com.flowboard.api_gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inject mock values for testing using ReflectionTestUtils
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "MyVerySecureSecretKeyForJWTSigningThatIsAtLeast32CharactersLongForHS256Algorithm");
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 3600000L); // 1 hour
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtUtil.generateToken("test@example.com", "ROLE_USER", 1L);
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testExtractEmail() {
        String token = jwtUtil.generateToken("test@example.com", "ROLE_USER", 1L);
        assertEquals("test@example.com", jwtUtil.extractEmail(token));
    }

    @Test
    void testExtractRole() {
        String token = jwtUtil.generateToken("test@example.com", "ROLE_USER", 1L);
        assertEquals("ROLE_USER", jwtUtil.extractRole(token));
    }

    @Test
    void testExtractUserId() {
        String token = jwtUtil.generateToken("test@example.com", "ROLE_USER", 1L);
        assertEquals(1L, jwtUtil.extractUserId(token));
    }

    @Test
    void testValidateToken_InvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.string"));
    }
}
