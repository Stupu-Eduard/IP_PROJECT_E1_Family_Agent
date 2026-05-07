package com.familie.cheltuieli_familie.security.filter;

import com.familie.cheltuieli_familie.model.UserSession;
import com.familie.cheltuieli_familie.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionCookieFilter extends OncePerRequestFilter {

    private final UserSessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extragem session_id din Cookie
        String sessionId = null;
        if (request.getCookies() != null) {
            sessionId = Arrays.stream(request.getCookies())
                    .filter(cookie -> "session_id".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        // 2. Verificăm în baza de date dacă sesiunea există și este validă
        if (sessionId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<UserSession> sessionOpt = sessionRepository.findBySessionToken(sessionId);

            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();

                if (session.isValid()) {
                    // Sesiune validă -> Setăm autentificarea în contextul Spring Security
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            session.getUser(),
                            null,
                            Collections.emptyList() // Aici poți adăuga rolurile din User dacă este nevoie
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("✅ Sesiuine validă găsită în DB pentru user: {}", session.getUser().getEmail());
                } else {
                    log.warn("⚠️ Sesiune expirată în DB: {}", sessionId);
                }
            } else {
                log.warn("❌ Sesiune inexistentă în DB: {}", sessionId);
            }
        }

        filterChain.doFilter(request, response);
    }
}
