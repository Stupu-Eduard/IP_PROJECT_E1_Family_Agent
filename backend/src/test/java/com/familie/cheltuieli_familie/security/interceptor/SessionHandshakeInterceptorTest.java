package com.familie.cheltuieli_familie.security.interceptor;

import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.TokenBlacklistService;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionHandshakeInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenBlacklistService blacklistService;

    @Mock
    private WebSocketHandler wsHandler;

    @InjectMocks
    private SessionHandshakeInterceptor interceptor;

    @Test
    @DisplayName("🟢 Should allow handshake when valid JWT token exists in query params")
    void beforeHandshake_WhenValidTokenInQueryParams_ReturnsTrue() {
        // GIVEN
        String token = "valid-token";
        String email = "test@familie.com";
        String jti = "jti-123";
        
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setQueryString("token=" + token);
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        User user = new User();
        user.setEmail(email);

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractJti(token)).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtUtil.validateToken(token, email)).thenReturn(true);
        
        Map<String, Object> attributes = new HashMap<>();

        // WHEN
        boolean result = interceptor.beforeHandshake(request, null, wsHandler, attributes);

        // THEN
        assertTrue(result);
        assertEquals(user, attributes.get("user"));
    }

    @Test
    @DisplayName("❌ Should return false when token is missing")
    void beforeHandshake_WhenTokenIsMissing_ReturnsFalse() {
        // GIVEN
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        // WHEN
        boolean result = interceptor.beforeHandshake(request, null, wsHandler, new HashMap<>());

        // THEN
        assertFalse(result);
    }

    @Test
    @DisplayName("❌ Should return false when token is blacklisted")
    void beforeHandshake_WhenTokenIsBlacklisted_ReturnsFalse() {
        // GIVEN
        String token = "blacklisted-token";
        String email = "test@familie.com";
        String jti = "jti-blacklisted";

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setQueryString("token=" + token);
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractJti(token)).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(true);

        // WHEN
        boolean result = interceptor.beforeHandshake(request, null, wsHandler, new HashMap<>());

        // THEN
        assertFalse(result);
    }
}
