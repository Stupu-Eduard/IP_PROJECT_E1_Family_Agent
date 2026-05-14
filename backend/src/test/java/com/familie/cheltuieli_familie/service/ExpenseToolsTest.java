package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.ExpenseEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseToolsTest {

    @Mock
    private ExpenseAnalyticsService analyticsService;

    @InjectMocks
    private ExpenseTools expenseTools;

    @Test
    void testCalculateTotal() {
        when(analyticsService.calculateTotal(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
                .thenReturn(new BigDecimal("500.00"));

        String result = expenseTools.calculateTotal("2024-01-01", "2024-01-31");

        assertEquals("Total expenses: 500.00 RON", result);
    }

    @Test
    void testCompareMembers() {
        when(analyticsService.compareMembers(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
                .thenReturn(Map.of("Teodor", new BigDecimal("300.00"), "Maria", new BigDecimal("200.00")));

        String result = expenseTools.compareMembers("2024-01-01", "2024-01-31");

        assertTrue(result.contains("Teodor: 300.00 RON"));
        assertTrue(result.contains("Maria: 200.00 RON"));
    }

    @Test
    void testDetectAnomalies() {
        ExpenseEntity expense = ExpenseEntity.builder()
                .amount(new BigDecimal("500.00"))
                .category("Electronics")
                .date(LocalDate.of(2024, 1, 15))
                .build();

        when(analyticsService.detectAnomalies(new BigDecimal("200"))).thenReturn(List.of(expense));

        String result = expenseTools.detectAnomalies("200");

        assertTrue(result.contains("Electronics"));
        assertTrue(result.contains("500.00 RON"));
    }

    @Test
    void testByCategory() {
        when(analyticsService.byCategory(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
                .thenReturn(Map.of("Food", new BigDecimal("300.00"), "Transport", new BigDecimal("100.00")));

        String result = expenseTools.byCategory("2024-01-01", "2024-01-31");

        assertTrue(result.contains("Food: 300.00 RON"));
        assertTrue(result.contains("Transport: 100.00 RON"));
    }

    @Test
    void testByPerson() {
        ExpenseEntity expense = ExpenseEntity.builder()
                .amount(new BigDecimal("150.00"))
                .category("Food")
                .date(LocalDate.of(2024, 1, 10))
                .build();

        when(analyticsService.findByPerson("Teodor", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
                .thenReturn(List.of(expense));

        String result = expenseTools.byPerson("Teodor", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("Teodor"));
        assertTrue(result.contains("150.00 RON"));
    }

    @Test
    void testComparePeriods() {
        when(analyticsService.calculateTotal(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
                .thenReturn(new BigDecimal("500.00"));
        when(analyticsService.calculateTotal(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)))
                .thenReturn(new BigDecimal("600.00"));

        String result = expenseTools.comparePeriods("2024-01-01", "2024-01-31", "2024-02-01", "2024-02-29");

        assertTrue(result.contains("500.00 RON"));
        assertTrue(result.contains("600.00 RON"));
    }

    @Test
    void testTopExpenses() {
        ExpenseEntity expense = ExpenseEntity.builder()
                .amount(new BigDecimal("400.00"))
                .category("Electronics")
                .person("Teodor")
                .date(LocalDate.of(2024, 1, 5))
                .build();

        when(analyticsService.getTopExpenses(3)).thenReturn(List.of(expense));

        String result = expenseTools.topExpenses("3");

        assertTrue(result.contains("400.00 RON"));
        assertTrue(result.contains("Electronics"));
    }

    @Test
    void testMonthlyAverage() {
        when(analyticsService.calculateMonthlyAverage(3)).thenReturn(new BigDecimal("450.00"));

        String result = expenseTools.monthlyAverage("3");

        assertEquals("Monthly average for the last 3 months: 450.00 RON", result);
    }

    @Test
    void testDescribeTrend() {
        when(analyticsService.calculateTrend("Food", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
                .thenReturn("Spending on Food increased by 10%");

        String result = expenseTools.describeTrend("Food", "2024-01-01", "2024-01-31");

        assertEquals("Spending on Food increased by 10%", result);
    }

    static Stream<Arguments> visualDescriptionSource() {
        return Stream.of(
            Arguments.of("Spending increased by 15.5%", "Trendul arată o creștere de 15.5% pentru Food"),
            Arguments.of("Spending decreased by 8.2%", "Trendul arată o scădere de 8.2% pentru Food"),
            Arguments.of("Spending remained stable", "Trend stabil pentru Food")
        );
    }

    @ParameterizedTest
    @MethodSource("visualDescriptionSource")
    void testGetVisualDescription(String trend, String expected) {
        when(analyticsService.calculateTrend("Food", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
                .thenReturn(trend);

        String result = expenseTools.getVisualDescription("Food", "2024-01-01", "2024-01-31");

        assertEquals(expected, result);
    }
}
