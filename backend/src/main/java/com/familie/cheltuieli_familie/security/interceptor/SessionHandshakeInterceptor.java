package com.familie.cheltuieli_familie.security.interceptor;

import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.TokenBlacklistService;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final TokenBlacklistService blacklistService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = extractToken(servletRequest);
            
            if (token != null) {
                try {
                    String email = jwtUtil.extractEmail(token);
                    String jti = jwtUtil.extractJti(token);

                    if (email != null && !blacklistService.isBlacklisted(jti)) {
                        Optional<User> userOpt = userRepository.findByEmail(email);
                        
                        if (userOpt.isPresent() && jwtUtil.validateToken(token, email)) {
                            User user = userOpt.get();
                            log.info("🟢 Handshake permis pentru user: {}", email);
                            attributes.put("user", user);
                            List<FamilyMember> memberships = familyMemberRepository.findByUserId(user.getId());
                            if (!memberships.isEmpty() && memberships.get(0).getFamily() != null) {
                                attributes.put("familyId", memberships.get(0).getFamily().getId());
                                log.info("🟢 THE PIPE: familyId={} asociat sesiunii pentru user={}", memberships.get(0).getFamily().getId(), email);
                            } else {
                                log.warn("⚠️ THE PIPE: User {} nu are familie asociată.", email);
                            }
                            return true;
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ Eroare validare JWT la handshake: {}", e.getMessage());
                }
            }
        }

        log.warn("🔴 Handshake refuzat: Token invalid sau inexistent.");
        return false;
    }

    private String extractToken(ServletServerHttpRequest request) {
        // 1. Încercăm din query params (standard pentru WS)
        String query = request.getServletRequest().getQueryString();
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];
        }

        // 2. Încercăm din header-ul Sec-WebSocket-Protocol sau Authorization (mai rar suportat direct de browsere pentru WS)
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Această metodă este lăsată goală intenționat deoarece nu este nevoie de logică post-handshake momentan.
    }
}
