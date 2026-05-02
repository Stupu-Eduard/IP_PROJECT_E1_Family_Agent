package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.LoginRequest;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.model.UserSession;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.repository.UserSessionRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository sessionRepository;

    @InjectMocks
    private AuthController authController;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        response = new MockHttpServletResponse();
    }

    @Test
    void login_CandDateSuntCorecte_CreeazaSesiuneSiReturneazaCookie() {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setEmail("test@familie.com");
        request.setPassword("parolaBuna");

        User user = new User();
        user.setName("Edi");
        user.setEmail("test@familie.com");
        user.setPasswordH("parolaBuna");

        when(userRepository.findByEmail("test@familie.com")).thenReturn(Optional.of(user));

        // WHEN
        ResponseEntity<?> result = authController.login(request, response);

        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertInstanceOf(Map.class, result.getBody());
        assertEquals("Edi", ((Map<?, ?>) result.getBody()).get("userName"));
        
        // Verificam cookie-ul creat
        Cookie sessionCookie = response.getCookie("session_id");
        assertNotNull(sessionCookie);
        assertTrue(sessionCookie.isHttpOnly());
        assertEquals("/", sessionCookie.getPath());
        assertEquals(24 * 60 * 60, sessionCookie.getMaxAge());

        // Verificam ca s-a salvat sesiunea in DB
        verify(sessionRepository, times(1)).save(any(UserSession.class));
    }

    @Test
    void login_CandParolaGresita_ReturneazaUnauthorizedSiNuSalveazaNimic() {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setEmail("test@familie.com");
        request.setPassword("parolaGresita");

        User user = new User();
        user.setEmail("test@familie.com");
        user.setPasswordH("parolaBuna");

        when(userRepository.findByEmail("test@familie.com")).thenReturn(Optional.of(user));

        // WHEN
        ResponseEntity<?> result = authController.login(request, response);

        // THEN
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertNull(response.getCookie("session_id"));
        verify(sessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void login_CandUserNuExista_ReturneazaUnauthorized() {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setEmail("inexistent@familie.com");
        request.setPassword("parola");

        when(userRepository.findByEmail("inexistent@familie.com")).thenReturn(Optional.empty());

        // WHEN
        ResponseEntity<?> result = authController.login(request, response);

        // THEN
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertNull(response.getCookie("session_id"));
        verify(sessionRepository, never()).save(any(UserSession.class));
    }
}
