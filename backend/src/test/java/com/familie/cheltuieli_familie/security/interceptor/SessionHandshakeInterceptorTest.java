package com.familie.cheltuieli_familie.security.interceptor;

import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.TokenBlacklistService;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.List;
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
    private FamilyMemberRepository familyMemberRepository;
    @Mock
    private TokenBlacklistService blacklistService;
    @Mock
    private WebSocketHandler wsHandler;

    @InjectMocks
    private SessionHandshakeInterceptor interceptor;

    @Test
    @DisplayName("🟢 Should allow handshake when valid JWT token exists in query params")
    void beforeHandshake_WhenValidTokenInQuery_ReturnsTrue() {
        // GIVEN
        String token = "valid-token";
        String email = "test@familie.com";
        String jti = "jti-123";

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setQueryString("token=" + token);
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        User user = new User();
        user.setId(5L);
        user.setEmail(email);

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractJti(token)).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtUtil.validateToken(token, email)).thenReturn(true);
        when(familyMemberRepository.findByUserId(5L)).thenReturn(List.of());

        Map<String, Object> attributes = new HashMap<>();

        // WHEN
        boolean result = interceptor.beforeHandshake(request, null, wsHandler, attributes);

        // THEN
        assertTrue(result);
        assertEquals(user, attributes.get("user"));
    }

    @Test
    @DisplayName("🟢 Should store familyId in attributes when user has a family")
    void beforeHandshake_WhenUserHasFamily_StoresFamilyId() {
        // GIVEN
        String token = "valid-token-with-family";
        String email = "parent@familie.com";
        String jti = "jti-family";

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setQueryString("token=" + token);
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        User user = new User();
        user.setId(10L);
        user.setEmail(email);

        Family family = new Family();
        family.setId(42L);

        FamilyMember membership = new FamilyMember();
        membership.setFamily(family);

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractJti(token)).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtUtil.validateToken(token, email)).thenReturn(true);
        when(familyMemberRepository.findByUserId(10L)).thenReturn(List.of(membership));

        Map<String, Object> attributes = new HashMap<>();

        // WHEN
        boolean result = interceptor.beforeHandshake(request, null, wsHandler, attributes);

        // THEN
        assertTrue(result);
        assertEquals(42L, attributes.get("familyId"));
    }

    @Test
    @DisplayName("🟢 Should allow handshake when valid JWT token exists in Authorization header")
    void beforeHandshake_WhenValidTokenInHeader_ReturnsTrue() {
        // GIVEN
        String token = "valid-header-token";
        String email = "test-header@familie.com";
        String jti = "jti-header";
        
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("Authorization", "Bearer " + token);
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
    @DisplayName("❌ Should return false when no token is provided")
    void beforeHandshake_WhenNoToken_ReturnsFalse() {
        // GIVEN
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        // WHEN
        boolean result = interceptor.beforeHandshake(request, null, wsHandler, new HashMap<>());

        // THEN
        assertFalse(result);
    }

    @Test
    @DisplayName("❌ Should return false when request is not ServletServerHttpRequest")
    void beforeHandshake_WhenNotServletRequest_ReturnsFalse() {
        // GIVEN
        ServerHttpRequest request = mock(ServerHttpRequest.class);

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

    @Test
    @DisplayName("❌ Should return false when token validation throws exception")
    void beforeHandshake_WhenExceptionOccurs_ReturnsFalse() {
        // GIVEN
        String token = "faulty-token";
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setQueryString("token=" + token);
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        when(jwtUtil.extractEmail(token)).thenThrow(new RuntimeException("Parsing failed"));

        // WHEN
        boolean result = interceptor.beforeHandshake(request, null, wsHandler, new HashMap<>());

        // THEN
        assertFalse(result);
    }
}
