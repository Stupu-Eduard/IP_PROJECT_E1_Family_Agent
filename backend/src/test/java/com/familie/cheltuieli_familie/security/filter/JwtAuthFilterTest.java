package com.familie.cheltuieli_familie.security.filter;

import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.TokenBlacklistService;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenBlacklistService blacklistService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotAuthenticateWithoutHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotAuthenticateWithInvalidHeaderFormat() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotAuthenticateIfBlacklisted() throws ServletException, IOException {
        String token = "blacklisted-token";
        String jti = "bad-jti";
        String email = "test@example.com";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractJti(token)).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldAuthenticateValidTokenWithRole() throws ServletException, IOException {
        String token = "valid-token";
        String jti = "good-jti";
        String email = "admin@test.com";
        User user = new User();
        user.setEmail(email);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractJti(token)).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtUtil.validateToken(token, email)).thenReturn(true);
        when(jwtUtil.extractClaim(anyString(), any())).thenReturn("ADMIN");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(1, SecurityContextHolder.getContext().getAuthentication().getAuthorities().size());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void shouldNotAuthenticateIfUserNotFound() throws ServletException, IOException {
        String token = "valid-token";
        String email = "missing@test.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractJti(token)).thenReturn("jti");
        when(blacklistService.isBlacklisted(anyString())).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotAuthenticateIfTokenValidationFails() throws ServletException, IOException {
        String token = "invalid-token";
        String email = "test@example.com";
        User user = new User();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractJti(token)).thenReturn("jti");
        when(blacklistService.isBlacklisted(anyString())).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtUtil.validateToken(token, email)).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldHandleExceptionsGracefully() throws ServletException, IOException {
        String token = "error-token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractEmail(token)).thenThrow(new RuntimeException("JWT error"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotAuthenticateIfAlreadyAuthenticated() throws ServletException, IOException {
        String token = "valid-token";
        String email = "test@example.com";
        UsernamePasswordAuthenticationToken existingAuth = new UsernamePasswordAuthenticationToken("user", null);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractEmail(token)).thenReturn(email);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(existingAuth, SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(blacklistService);
    }

    @Test
    void shouldAuthenticateWithoutRole() throws ServletException, IOException {
        String token = "valid-token";
        String jti = "good-jti";
        String email = "user@test.com";
        User user = new User();
        user.setEmail(email);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractJti(token)).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtUtil.validateToken(token, email)).thenReturn(true);
        when(jwtUtil.extractClaim(anyString(), any())).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().isEmpty());
    }
}
