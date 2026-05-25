package com.familie.cheltuieli_familie.security.interceptor;

import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.TokenBlacklistService;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.util.Collections;
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

    @InjectMocks
    private SessionHandshakeInterceptor interceptor;

    @Mock
    private ServerHttpRequest serverRequest;

    @Mock
    private ServerHttpResponse serverResponse;

    @Mock
    private WebSocketHandler wsHandler;

    @Test
    void beforeHandshake_shouldRejectNonServletRequest() {
        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(serverRequest, serverResponse, wsHandler, attributes);
        assertFalse(result);
    }

    @Test
    void beforeHandshake_shouldRejectMissingToken() {
        ServletServerHttpRequest servletRequest = mock(ServletServerHttpRequest.class);
        jakarta.servlet.http.HttpServletRequest httpReq = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(servletRequest.getServletRequest()).thenReturn(httpReq);
        when(httpReq.getQueryString()).thenReturn(null);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        when(servletRequest.getHeaders()).thenReturn(headers);

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(servletRequest, serverResponse, wsHandler, attributes);
        assertFalse(result);
    }

    @Test
    void beforeHandshake_shouldRejectBlacklistedToken() {
        ServletServerHttpRequest servletRequest = mock(ServletServerHttpRequest.class);
        jakarta.servlet.http.HttpServletRequest httpReq = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(servletRequest.getServletRequest()).thenReturn(httpReq);
        when(httpReq.getQueryString()).thenReturn("token=blacklisted_token");

        when(jwtUtil.extractEmail("blacklisted_token")).thenReturn("test@example.com");
        when(jwtUtil.extractJti("blacklisted_token")).thenReturn("jti123");
        when(blacklistService.isBlacklisted("jti123")).thenReturn(true);

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(servletRequest, serverResponse, wsHandler, attributes);
        assertFalse(result);
    }

    @Test
    void beforeHandshake_shouldAcceptValidToken() {
        ServletServerHttpRequest servletRequest = mock(ServletServerHttpRequest.class);
        jakarta.servlet.http.HttpServletRequest httpReq = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(servletRequest.getServletRequest()).thenReturn(httpReq);
        when(httpReq.getQueryString()).thenReturn("token=valid_token");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(jwtUtil.extractEmail("valid_token")).thenReturn("test@example.com");
        when(jwtUtil.extractJti("valid_token")).thenReturn("jti456");
        when(blacklistService.isBlacklisted("jti456")).thenReturn(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.validateToken("valid_token", "test@example.com")).thenReturn(true);
        when(familyMemberRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(servletRequest, serverResponse, wsHandler, attributes);
        assertTrue(result);
        assertEquals(user, attributes.get("user"));
    }

    @Test
    void beforeHandshake_shouldSetFamilyIdWhenAvailable() {
        ServletServerHttpRequest servletRequest = mock(ServletServerHttpRequest.class);
        jakarta.servlet.http.HttpServletRequest httpReq = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(servletRequest.getServletRequest()).thenReturn(httpReq);
        when(httpReq.getQueryString()).thenReturn("token=valid_token");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Family family = new Family();
        family.setId(10L);

        FamilyMember member = new FamilyMember();
        member.setUser(user);
        member.setFamily(family);

        when(jwtUtil.extractEmail("valid_token")).thenReturn("test@example.com");
        when(jwtUtil.extractJti("valid_token")).thenReturn("jti789");
        when(blacklistService.isBlacklisted("jti789")).thenReturn(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.validateToken("valid_token", "test@example.com")).thenReturn(true);
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(member));

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(servletRequest, serverResponse, wsHandler, attributes);
        assertTrue(result);
        assertEquals(10L, attributes.get("familyId"));
    }

    @Test
    void beforeHandshake_shouldExtractTokenFromAuthorizationHeader() {
        ServletServerHttpRequest servletRequest = mock(ServletServerHttpRequest.class);
        jakarta.servlet.http.HttpServletRequest httpReq = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(servletRequest.getServletRequest()).thenReturn(httpReq);
        when(httpReq.getQueryString()).thenReturn(null);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Authorization", "Bearer header_token");
        when(servletRequest.getHeaders()).thenReturn(headers);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(jwtUtil.extractEmail("header_token")).thenReturn("test@example.com");
        when(jwtUtil.extractJti("header_token")).thenReturn("jti000");
        when(blacklistService.isBlacklisted("jti000")).thenReturn(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.validateToken("header_token", "test@example.com")).thenReturn(true);
        when(familyMemberRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(servletRequest, serverResponse, wsHandler, attributes);
        assertTrue(result);
    }

    @Test
    void afterHandshake_shouldNotThrow() {
        assertDoesNotThrow(() -> interceptor.afterHandshake(serverRequest, serverResponse, wsHandler, null));
    }
}
