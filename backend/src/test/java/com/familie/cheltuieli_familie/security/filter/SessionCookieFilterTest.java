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

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        // Curățăm contextul și instanțiem obiectele HTTP o singură dată pentru toate testele
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    // 1. Cazul ideal: Sesiune validă în DB
    @Test
    void doFilter_CandCookieExistaSiSesiuneValida_SeteazaAutentificarea() throws Exception {
        String sessionId = "test-session-123";
        request.setCookies(new Cookie("session_id", sessionId));

        User user = new User();
        user.setEmail("test@familie.com");

        UserSession session = mock(UserSession.class);
        when(session.isValid()).thenReturn(true);
        when(session.getUser()).thenReturn(user);

        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.of(session));

        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    // 2. Nu avem deloc Cookie-uri
    @Test
    void doFilter_CandCookieLipseste_ContinuaLantulFaraAutentificare() throws Exception {
        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(sessionRepository);
    }

    // 3. Avem Cookie-uri, dar niciunul nu se numește "session_id"
    @Test
    void doFilter_CandExistaAlteCookiesDarNuSessionId_ContinuaFaraAutentificare() throws Exception {
        request.setCookies(new Cookie("un_alt_cookie", "valoare"));

        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(sessionRepository);
    }

    // 4. Avem "session_id", dar nu este găsit în Baza de Date
    @Test
    void doFilter_CandSesiuneaNuExistaInDB_ContinuaLantulFaraAutentificare() throws Exception {
        String sessionId = "unknown-session";
        request.setCookies(new Cookie("session_id", sessionId));

        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.empty());

        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // 5. Sesiunea există în DB, dar este marcată ca expirată/invalidă
    @Test
    void doFilter_CandSesiuneaEsteExpirata_NuSeteazaAutentificarea() throws Exception {
        String sessionId = "expired-session";
        request.setCookies(new Cookie("session_id", sessionId));

        UserSession session = mock(UserSession.class);
        when(session.isValid()).thenReturn(false); // Simulăm sesiunea expirată

        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.of(session));

        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // 6. Requestul vine, dar userul este deja autentificat în SecurityContext
    @Test
    void doFilter_CandAutentificareaExistaDeja_NuMaiVerificaInDB() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("already-auth", null)
        );
        request.setCookies(new Cookie("session_id", "some-id"));

        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(sessionRepository);
        verify(filterChain).doFilter(request, response);
    }
}