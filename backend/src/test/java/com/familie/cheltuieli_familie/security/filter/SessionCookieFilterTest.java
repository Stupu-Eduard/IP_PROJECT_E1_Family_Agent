package com.familie.cheltuieli_familie.security.filter;

import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.model.UserSession;
import com.familie.cheltuieli_familie.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionCookieFilterTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SessionCookieFilter sessionCookieFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_CandCookieExistaSiSesiuneValida_SeteazaAutentificarea() throws Exception {
        // GIVEN
        String sessionId = "test-session-123";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("session_id", sessionId));
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = new User();
        user.setEmail("test@familie.com");
        
        UserSession session = UserSession.builder()
                .id(1L)
                .sessionToken(sessionId)
                .user(user)
                .lastActive(LocalDateTime.now())
                .build();

        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.of(session));

        // WHEN
        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        // THEN
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_CandCookieLipseste_ContinuaLantulFaraAutentificare() throws Exception {
        // GIVEN
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // WHEN
        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        // THEN
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_CandSesiuneaNuExistaInDB_ContinuaLantulFaraAutentificare() throws Exception {
        // GIVEN
        String sessionId = "unknown-session";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("session_id", sessionId));
        
        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.empty());

        // WHEN
        sessionCookieFilter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        // THEN
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_CandAutentificareaExistaDeja_NuMaiVerificaInDB() throws Exception {
        // GIVEN
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("already-auth", null)
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("session_id", "some-id"));

        // WHEN
        sessionCookieFilter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        // THEN
        verifyNoInteractions(sessionRepository);
    }
}
