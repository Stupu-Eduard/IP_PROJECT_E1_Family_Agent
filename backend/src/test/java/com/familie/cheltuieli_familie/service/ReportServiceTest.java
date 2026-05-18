package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.config.LlmConfig;
import com.familie.cheltuieli_familie.security.util.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ExpenseAnalyticsService analyticsService;

    @Mock
    private LlmConfig.ReportAssistant reportAssistant;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        when(securityService.resolveScope()).thenReturn(new Long[]{1L, 10L});
    }

    @Test
    void generateMonthlySummary_shouldReturnCorrectString() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        
        when(analyticsService.calculateTotal(eq(from), eq(to), anyLong(), anyLong()))
                .thenReturn(new BigDecimal("100.50"));
        when(analyticsService.byCategory(eq(from), eq(to), anyLong(), anyLong()))
                .thenReturn(Map.of("Food", new BigDecimal("100.50")));
        when(analyticsService.calculateTrend(anyString(), eq(from), eq(to), anyLong(), anyLong()))
                .thenReturn("Spending increased by 10%");

        String result = reportService.generateMonthlySummary(2026, 5);

        assertNotNull(result);
        assertTrue(result.contains("May 2026"));
        assertTrue(result.contains("100.50 RON"));
        assertTrue(result.contains("Food: 100.50 RON"));
        assertTrue(result.contains("Top Category: Food"));
        assertTrue(result.contains("Trend: Spending increased by 10%"));
    }

    @Test
    void generateNarrativeReport_shouldCallAssistant() {
        when(reportAssistant.generateReport(anyString())).thenReturn("Narrative Report Content");
        
        // Mocking dependency calls for generateMonthlySummary which is called inside generateNarrativeReport
        when(analyticsService.calculateTotal(any(), any(), anyLong(), anyLong())).thenReturn(BigDecimal.ZERO);
        when(analyticsService.byCategory(any(), any(), anyLong(), anyLong())).thenReturn(Map.of());

        String result = reportService.generateNarrativeReport(2026, 5);

        assertEquals("Narrative Report Content", result);
        verify(reportAssistant).generateReport(anyString());
    }

    @Test
    void generateMonthlySummary_shouldHandleEmptyData() {
        when(analyticsService.calculateTotal(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(analyticsService.byCategory(any(), any(), any(), any()))
                .thenReturn(Map.of());

        String result = reportService.generateMonthlySummary(2026, 5);

        assertNotNull(result);
        assertTrue(result.contains("Total Spent: 0 RON"));
        assertFalse(result.contains("Top Category"));
    }
}
