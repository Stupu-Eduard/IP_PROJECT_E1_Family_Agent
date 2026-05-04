package com.familie.cheltuieli_familie.security.filter;

/*import com.familie.cheltuieli_familie.repository.AlertRepository; // Asigură-te că folosești repository-ul tău real
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
//
//@Component
public class CustomCsrfFilter extends OncePerRequestFilter {

    private final AlertRepository alertRepository;

    // Injectăm repository-ul corect definit în proiect
    public CustomCsrfFilter(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();

        if (isModifyingMethod(method) && !isExemptRoute(request.getRequestURI())) {

            String csrfHeader = request.getHeader("X-Custom-CSRF");

            boolean isValid = false;
            if (csrfHeader != null && !csrfHeader.isEmpty()) {
                // Verificăm în baza de date folosind repository-ul corect
                isValid = alertRepository.existsByRestrictedCategory(csrfHeader);
            }

            if (!isValid) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Eroare de Securitate: Token Anti-CSRF invalid sau lipsa în baza de date!");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isModifyingMethod(String method) {
        return "POST".equalsIgnoreCase(method) ||
                "PUT".equalsIgnoreCase(method) ||
                "DELETE".equalsIgnoreCase(method) ||
                "PATCH".equalsIgnoreCase(method);
    }

    private boolean isExemptRoute(String uri) {
        return uri.startsWith("/api/v1/auth") ||
                uri.startsWith("/api/ws") ||
                uri.startsWith("/api/v1/demo");
    }
}*/