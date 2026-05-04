package com.familie.cheltuieli_familie.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CustomCsrfFilter extends OncePerRequestFilter {

    // Aici injectezi repository-ul tău dacă vrei să verifici direct în DB
    // private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();

        // CSRF se aplică doar metodelor care modifică date (POST, PUT, DELETE)
        // Nu aplicăm pe GET sau pe rutele de autentificare/websockets
        if (isModifyingMethod(method) && !isExemptRoute(request.getRequestURI())) {

            // Extragem token-ul trimis de frontend în header
            String csrfHeader = request.getHeader("X-Custom-CSRF");

            //  Verificarea cu baza de date
            // Exemplu: boolean isValid = tokenRepository.existsByTokenAndSessionActive(csrfHeader);
            boolean isValid = (csrfHeader != null && !csrfHeader.isEmpty()); // Acum doar verificăm că există

            if (!isValid) {
                // Dacă nu are token, îi dăm cu ușa în nas (403 Forbidden)
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Eroare de Securitate: Token Anti-CSRF invalid sau lipsa!");
                return;
            }
        }

        // Dacă totul e ok, lăsăm cererea să treacă mai departe
        filterChain.doFilter(request, response);
    }

    private boolean isModifyingMethod(String method) {
        return "POST".equalsIgnoreCase(method) ||
                "PUT".equalsIgnoreCase(method) ||
                "DELETE".equalsIgnoreCase(method);
    }

    private boolean isExemptRoute(String uri) {
        return uri.startsWith("/api/v1/auth") ||
                uri.startsWith("/api/ws") ||
                uri.startsWith("/api/v1/demo"); // Rutele care nu au nevoie de protectie
    }
}