package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.LoginRequest;
import com.familie.cheltuieli_familie.dto.RegisterRequest;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.TokenBlacklistService;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService blacklistService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void login_CandDateSuntCorecte_ReturneazaToken() {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setEmail("test@familie.com");
        request.setPassword("parolaBuna");

        User user = new User();
        user.setId(1L);
        user.setName("Edi");
        user.setEmail("test@familie.com");
        user.setPasswordH("parolaBuna");

        when(userRepository.findByEmail("test@familie.com")).thenReturn(Optional.of(user));
        when(familyMemberRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(jwtUtil.generateToken(eq("test@familie.com"), any())).thenReturn("mock-token");

        // WHEN
        ResponseEntity<Object> result = authController.login(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertNotNull(body);
        assertEquals("mock-token", body.get("token"));
        assertEquals("Edi", body.get("userName"));
        assertEquals("Parent", body.get("role"));
    }

    @Test
    void login_CandParolaGresita_ReturneazaUnauthorized() {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setEmail("test@familie.com");
        request.setPassword("parolaGresita");

        User user = new User();
        user.setEmail("test@familie.com");
        user.setPasswordH("parolaBuna");

        when(userRepository.findByEmail("test@familie.com")).thenReturn(Optional.of(user));

        // WHEN
        ResponseEntity<Object> result = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void logout_CandTokenEsteValid_IlAdaugaInBlacklist() {
        // GIVEN
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.extractJti("valid-token")).thenReturn("jti-123");
        when(jwtUtil.extractExpiration("valid-token")).thenReturn(new Date(System.currentTimeMillis() + 10000));

        // WHEN
        ResponseEntity<Object> result = authController.logout(request);

        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(blacklistService, times(1)).revokeToken(eq("jti-123"), any(Date.class));
    }

    @Test
    void register_whenEmailAlreadyExists_returnsConflict() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Ana");
        request.setEmail("ana@familie.com");
        request.setPassword("password123");

        User existingUser = new User();
        existingUser.setEmail("ana@familie.com");
        when(userRepository.findByEmail("ana@familie.com")).thenReturn(Optional.of(existingUser));

        ResponseEntity<Object> result = authController.register(request, response);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
        verify(sessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void register_whenRequestIsValid_returnsCreatedAndSessionHeader() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Ana");
        request.setEmail("ana@familie.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("ana@familie.com")).thenReturn(Optional.empty());

        ResponseEntity<Object> result = authController.register(request, response);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertInstanceOf(Map.class, result.getBody());
        assertEquals("Ana", ((Map<?, ?>) result.getBody()).get("userName"));
        assertNotNull(((Map<?, ?>) result.getBody()).get("token"));
        assertNotNull(response.getHeader("Set-Cookie"));
        assertTrue(response.getHeader("Set-Cookie").contains("session_id="));
        verify(userRepository, times(1)).save(any(User.class));
        verify(sessionRepository, times(1)).save(any(UserSession.class));
    }
}
