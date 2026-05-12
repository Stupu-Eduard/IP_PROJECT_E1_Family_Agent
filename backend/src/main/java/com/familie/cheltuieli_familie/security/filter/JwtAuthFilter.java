package com.familie.cheltuieli_familie.security.filter;

import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.TokenBlacklistService;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final TokenBlacklistService blacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            userEmail = jwtUtil.extractEmail(jwt);
            String jti = jwtUtil.extractJti(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (blacklistService.isBlacklisted(jti)) {
                    log.warn("⚠️ Token revocat (blacklist) detectat pentru: {}", userEmail);
                    filterChain.doFilter(request, response);
                    return;
                }

                Optional<User> userOpt = userRepository.findByEmail(userEmail);

                if (userOpt.isPresent() && jwtUtil.validateToken(jwt, userEmail)) {
                    User user = userOpt.get();
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            Collections.emptyList() // Aici poți adăuga autoritățile dacă există
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("✅ JWT Valid pentru user: {}", userEmail);
                }
            }
        } catch (Exception e) {
            log.error("❌ Eroare la validarea JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
