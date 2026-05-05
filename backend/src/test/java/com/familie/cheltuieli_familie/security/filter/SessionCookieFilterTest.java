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
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    // 1. Fără cookie-uri
    @Test
    void doFilter_FaraCookies_ContinuaLantul() throws Exception {
        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(sessionRepository);
    }

    // 2. Are cookie-uri, dar lipsește "session_id"
    @Test
    void doFilter_AlteCookiesFaraSessionId_ContinuaLantul() throws Exception {
        request.setCookies(new Cookie("alt_cookie", "123"));

        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // 3. User deja autentificat
    @Test
    void doFilter_UserDejaAutentificat_SareVerificareaDb() throws Exception {
        request.setCookies(new Cookie("session_id", "token-123"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null)
        );

        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(sessionRepository);
    }

    // 4. Sesiune inexistentă în DB
    @Test
    void doFilter_SesiuneInexistenta_ContinuaLantul() throws Exception {
        String sessionId = "token-inexistent";
        request.setCookies(new Cookie("session_id", sessionId));
        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.empty());

        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // 5. Sesiune expirată/invalidă în DB
    @Test
    void doFilter_SesiuneInvalida_ContinuaLantul() throws Exception {
        String sessionId = "token-invalid";
        request.setCookies(new Cookie("session_id", sessionId));

        UserSession session = mock(UserSession.class);
        when(session.isValid()).thenReturn(false);
        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.of(session));

        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // 6. Metoda GET - Sesiune validă (Nu necesită CSRF)
    @Test
    void doFilter_MetodaGet_SesiuneValida_SeteazaAutentificare() throws Exception {
        String sessionId = "token-valid";
        request.setCookies(new Cookie("session_id", sessionId));
        request.setMethod("GET");

        User user = new User();
        user.setEmail("test@familie.com");

        UserSession session = mock(UserSession.class);
        when(session.isValid()).thenReturn(true);
        when(session.getUser()).thenReturn(user);

        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.of(session));

        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // 7. Metoda POST - Lipsă Header CSRF -> 403 Forbidden
    @Test
    void doFilter_MetodaPost_FaraCsrf_Returneaza403() throws Exception {
        String sessionId = "token-valid";
        request.setCookies(new Cookie("session_id", sessionId));
        request.setMethod("POST");

        UserSession session = mock(UserSession.class);
        when(session.isValid()).thenReturn(true);
        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.of(session));

        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid or missing CSRF Token"));
        verify(filterChain, never()).doFilter(request, response); // Oprește lanțul
    }

    // 8. Metoda POST - Header CSRF Invalid -> 403 Forbidden
    @Test
    void doFilter_MetodaPost_CsrfInvalid_Returneaza403() throws Exception {
        String sessionId = "token-valid";
        request.setCookies(new Cookie("session_id", sessionId));
        request.setMethod("POST");
        request.addHeader("X-XSRF-TOKEN", "token-gresit");

        UserSession session = mock(UserSession.class);
        when(session.isValid()).thenReturn(true);
        when(session.getCsrfToken()).thenReturn("token-corect");
        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.of(session));

        sessionCookieFilter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    // 9. Metode care modifică date cu CSRF Valid (Acoperim POST, PUT, DELETE, PATCH pentru coverage maxim)
    @Test
    void doFilter_MetodaPost_CsrfValid_SeteazaAutentificare() throws Exception {
        testValidMethodWithCsrf("POST");
    }

    @Test
    void doFilter_MetodaPut_CsrfValid_SeteazaAutentificare() throws Exception {
        testValidMethodWithCsrf("PUT");
    }

    @Test
    void doFilter_MetodaDelete_CsrfValid_SeteazaAutentificare() throws Exception {
        testValidMethodWithCsrf("DELETE");
    }

    @Test
    void doFilter_MetodaPatch_CsrfValid_SeteazaAutentificare() throws Exception {
        testValidMethodWithCsrf("PATCH");
    }

    // Metodă ajutătoare pentru a nu duplica codul la cele 4 teste de mai sus
    private void testValidMethodWithCsrf(String httpMethod) throws Exception {
        SecurityContextHolder.clearContext(); // Resetăm contextul pentru fiecare rulare
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        String sessionId = "token-valid";
        req.setCookies(new Cookie("session_id", sessionId));
        req.setMethod(httpMethod);
        req.addHeader("X-XSRF-TOKEN", "token-secret");

        User user = new User();
        user.setEmail("test@familie.com");

        UserSession session = mock(UserSession.class);
        when(session.isValid()).thenReturn(true);
        when(session.getCsrfToken()).thenReturn("token-secret");
        when(session.getUser()).thenReturn(user);

        when(sessionRepository.findBySessionToken(sessionId)).thenReturn(Optional.of(session));

        sessionCookieFilter.doFilterInternal(req, res, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(req, res);
    }
}