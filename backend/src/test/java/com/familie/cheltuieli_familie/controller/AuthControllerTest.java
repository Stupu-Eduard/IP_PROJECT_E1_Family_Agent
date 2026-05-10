package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.LoginRequest;
import com.familie.cheltuieli_familie.dto.RegisterRequest;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.model.UserSession;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.repository.UserSessionRepository;
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
    void login_whenCredentialsAreValid_returnsOkAndCreatesSessionHeader() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@familie.com");
        request.setPassword("parolaBuna");

        User user = new User();
        user.setName("Edi");
        user.setEmail("test@familie.com");
        user.setPasswordH("parolaBuna");

        when(userRepository.findByEmail("test@familie.com")).thenReturn(Optional.of(user));

        ResponseEntity<Object> result = authController.login(request, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertInstanceOf(Map.class, result.getBody());
        assertEquals("Edi", ((Map<?, ?>) result.getBody()).get("userName"));
        assertNotNull(response.getHeader("Set-Cookie"));
        assertTrue(response.getHeader("Set-Cookie").contains("session_id="));
        verify(sessionRepository, times(1)).save(any(UserSession.class));
    }

    @Test
    void login_whenPasswordIsInvalid_returnsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@familie.com");
        request.setPassword("parolaGresita");

        User user = new User();
        user.setEmail("test@familie.com");
        user.setPasswordH("parolaBuna");

        when(userRepository.findByEmail("test@familie.com")).thenReturn(Optional.of(user));

        ResponseEntity<Object> result = authController.login(request, response);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertNull(response.getHeader("Set-Cookie"));
        verify(sessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void login_whenUserDoesNotExist_returnsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("inexistent@familie.com");
        request.setPassword("parola");

        when(userRepository.findByEmail("inexistent@familie.com")).thenReturn(Optional.empty());

        ResponseEntity<Object> result = authController.login(request, response);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertNull(response.getHeader("Set-Cookie"));
        verify(sessionRepository, never()).save(any(UserSession.class));
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
