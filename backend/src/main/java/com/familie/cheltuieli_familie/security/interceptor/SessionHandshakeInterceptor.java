package com.familie.cheltuieli_familie.security.interceptor;

import com.familie.cheltuieli_familie.model.UserSession;
import com.familie.cheltuieli_familie.repository.UserSessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.WebUtils;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionHandshakeInterceptor implements HandshakeInterceptor {

    private final UserSessionRepository sessionRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest req = servletRequest.getServletRequest();
            
            // Extragem session_id din cookie
            Cookie cookie = WebUtils.getCookie(req, "session_id");
            
            if (cookie != null) {
                String sessionId = cookie.getValue();
                Optional<UserSession> sessionOpt = sessionRepository.findBySessionToken(sessionId);
                
                if (sessionOpt.isPresent() && sessionOpt.get().isValid()) {
                    log.info("🟢 Handshake permis pentru sesiunea validă: {}", sessionId);
                    attributes.put("user", sessionOpt.get().getUser());
                    return true;
                }
            } else {
                // FALLBACK TEMPORAR PENTRU DEMO: Permitem fara cookie pe localhost
                log.info("ℹ️ Handshake permis automat pentru testare locală (fără cookie)");
                return true;
            }
        }

        log.warn("🔴 Handshake refuzat: Sesiune invalidă sau inexistentă în DB.");
        return false; // Refuzăm conexiunea WebSocket
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nu este nevoie de logică aici
    }
}
