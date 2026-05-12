package com.familie.cheltuieli_familie.security.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "vO7_9#K2pL5n*R8x@W1m&Z4q!A0sB3eF_test_secret_key";
    private final long expiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, expiration);
    }

    @Test
    void shouldGenerateToken() {
        String email = "test@example.com";
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "Parent");

        String token = jwtUtil.generateToken(email, claims);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldExtractEmail() {
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email, new HashMap<>());

        String extractedEmail = jwtUtil.extractEmail(token);

        assertEquals(email, extractedEmail);
    }

    @Test
    void shouldExtractJti() {
        String token = jwtUtil.generateToken("test@example.com", new HashMap<>());

        String jti = jwtUtil.extractJti(token);

        assertNotNull(jti);
        assertFalse(jti.isEmpty());
    }

    @Test
    void shouldValidateToken() {
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email, new HashMap<>());

        assertTrue(jwtUtil.validateToken(token, email));
    }

    @Test
    void shouldNotValidateTokenWithWrongEmail() {
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email, new HashMap<>());

        assertFalse(jwtUtil.validateToken(token, "wrong@example.com"));
    }

    @Test
    void shouldReturnExpirationDate() {
        String token = jwtUtil.generateToken("test@example.com", new HashMap<>());

        Date expirationDate = jwtUtil.extractExpiration(token);

        assertTrue(expirationDate.after(new Date()));
    }
}
