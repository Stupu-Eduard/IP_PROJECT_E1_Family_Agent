package com.familie.cheltuieli_familie.security.filter;
/*
import com.familie.cheltuieli_familie.repository.AlertRepository; // Asigură-te că importul este corect pentru proiectul tău
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CustomCsrfFilterTest {

    private CustomCsrfFilter customCsrfFilter;
    private AlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        // Inițializăm un mock pentru repository-ul tău din proiect
        alertRepository = mock(AlertRepository.class);
        customCsrfFilter = new CustomCsrfFilter(alertRepository);
    }

    @Test
    void testExemptRoutePasses() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        request.setRequestURI("/api/v1/auth/login");
        request.setMethod("POST");

        customCsrfFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testModifyingMethodWithoutTokenFails() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        request.setRequestURI("/api/v1/expenses");
        request.setMethod("POST");

        customCsrfFilter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
    }
}*/