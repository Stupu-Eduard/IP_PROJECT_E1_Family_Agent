package com.familie.cheltuieli_familie.security.interceptor;

import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.model.UserSession;
import com.familie.cheltuieli_familie.repository.UserSessionRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionHandshakeInterceptorTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private WebSocketHandler wsHandler;

    @InjectMocks
    private SessionHandshakeInterceptor interceptor;

    @Test
    @DisplayName("🟢 Should allow handshake when valid session cookie exists")
    void beforeHandshake_WhenValidCookieExists_ReturnsTrue() {
        // GIVEN
        String sessionId = "ws-session-id";
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setCookies(new Cookie("session_id", sessionId));
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        User user = new User();
        UserSession session = UserSession.builder()
                .id(1L)
                .sessionToken(sessionId)
                .user(user)
                .lastActive(LocalDateTime.now())
                .build();

        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.of(session));
        Map<String, Object> attributes = new HashMap<>();

        // WHEN
        boolean result = interceptor.beforeHandshake(request, null, wsHandler, attributes);

        // THEN
        assertTrue(result);
        assertEquals(user, attributes.get("user"));
    }

    @Test
    @DisplayName("❌ Should return false when session cookie is missing")
    void beforeHandshake_WhenCookieIsMissing_ReturnsFalse() {
        // GIVEN
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        // WHEN
        boolean result = interceptor.beforeHandshake(request, null, wsHandler, new HashMap<>());

        // THEN
        assertFalse(result); // Trebuie sa fie false pentru securitate maxima
    }

    @Test
    @DisplayName("❌ Should return false when session does not exist in DB")
    void beforeHandshake_WhenSessionNotFound_ReturnsFalse() {
        // GIVEN
        String sessionId = "invalid-id";
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setCookies(new Cookie("session_id", sessionId));
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.empty());

        // WHEN
        boolean result = interceptor.beforeHandshake(request, null, wsHandler, new HashMap<>());

        // THEN
        assertFalse(result);
    }
}
