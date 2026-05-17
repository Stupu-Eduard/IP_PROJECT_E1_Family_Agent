package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
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
    private FamilyMemberRepository familyMemberRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void testGenerateMonthlySummary() {
        when(analyticsService.calculateTotal(eq(LocalDate.of(2024, 3, 1)), eq(LocalDate.of(2024, 3, 31)), any(), any()))
                .thenReturn(new BigDecimal("850.00"));
        when(analyticsService.byCategory(eq(LocalDate.of(2024, 3, 1)), eq(LocalDate.of(2024, 3, 31)), any(), any()))
                .thenReturn(Map.of("Food", new BigDecimal("500.00"), "Transport", new BigDecimal("350.00")));
        when(analyticsService.calculateTrend(any(), eq(LocalDate.of(2024, 3, 1)), eq(LocalDate.of(2024, 3, 31)), any(), any()))
                .thenReturn("Stable");

        String result = reportService.generateMonthlySummary(2024, 3);

        assertNotNull(result);
        assertTrue(result.contains("850.00"));
        assertTrue(result.contains("Food"));
        assertTrue(result.contains("Transport"));
        assertTrue(result.contains("Top Category"));
    }

    @Test
    void testGenerateMonthlySummaryEmpty() {
        when(analyticsService.calculateTotal(eq(LocalDate.of(2024, 2, 1)), eq(LocalDate.of(2024, 2, 29)), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(analyticsService.byCategory(eq(LocalDate.of(2024, 2, 1)), eq(LocalDate.of(2024, 2, 29)), any(), any()))
                .thenReturn(Map.of());

        String result = reportService.generateMonthlySummary(2024, 2);

        assertNotNull(result);
        assertTrue(result.contains("0"));
    }
}
