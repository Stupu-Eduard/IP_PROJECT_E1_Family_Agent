package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ExpenseAnalyticsServiceTest {

    @Mock
    private ExpenseJpaRepository repository;

    @InjectMocks
    private ExpenseAnalyticsService analyticsService;

    private List<Expense> sampleExpenses;

    @BeforeEach
    void setUp() {
        sampleExpenses = List.of(
                Expense.builder().amount(new BigDecimal("100.00")).aiCategory("Food").aiPerson("Alice").expenseDate(LocalDateTime.now()).build(),
                Expense.builder().amount(new BigDecimal("200.00")).aiCategory("Transport").aiPerson("Bob").expenseDate(LocalDateTime.now()).build(),
                Expense.builder().amount(new BigDecimal("150.00")).aiCategory("Food").aiPerson("Alice").expenseDate(LocalDateTime.now()).build()
        );
    }

    @Test
    void testCalculateTotal() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(repository.findByDateBetween(from, to)).thenReturn(sampleExpenses);

        BigDecimal total = analyticsService.calculateTotal(from, to);

        assertEquals(new BigDecimal("450.00"), total);
    }

    @Test
    void testByCategory() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(repository.findByDateBetween(from, to)).thenReturn(sampleExpenses);

        Map<String, BigDecimal> result = analyticsService.byCategory(from, to);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("250.00"), result.get("Food"));
        assertEquals(new BigDecimal("200.00"), result.get("Transport"));
    }

    @Test
    void testCompareMembers() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(repository.findByDateBetween(from, to)).thenReturn(sampleExpenses);

        Map<String, BigDecimal> result = analyticsService.compareMembers(from, to);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("250.00"), result.get("Alice"));
        assertEquals(new BigDecimal("200.00"), result.get("Bob"));
    }

    @Test
    void testDetectAnomalies() {
        when(repository.findAll()).thenReturn(sampleExpenses);

        List<Expense> anomalies = analyticsService.detectAnomalies(new BigDecimal("180.00"));

        assertEquals(1, anomalies.size());
        assertEquals(new BigDecimal("200.00"), anomalies.get(0).getAmount());
    }

    @Test
    void testFindByPerson() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(repository.findByDateBetween(from, to)).thenReturn(sampleExpenses);

        List<Expense> result = analyticsService.findByPerson("Alice", from, to);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> "Alice".equalsIgnoreCase(e.getAiPerson())));
    }

    @Test
    void testFindByPersonNoMatch() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(repository.findByDateBetween(from, to)).thenReturn(sampleExpenses);

        List<Expense> result = analyticsService.findByPerson("Charlie", from, to);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTopExpenses() {
        Page<Expense> page = new PageImpl<>(sampleExpenses);
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        List<Expense> result = analyticsService.getTopExpenses(3);

        assertEquals(3, result.size());
    }

    @Test
    void testCalculateMonthlyAverage() {
        LocalDate now = LocalDate.now();
        LocalDate from = now.minusMonths(3).withDayOfMonth(1);
        when(repository.findByDateBetween(any(LocalDate.class), eq(now))).thenReturn(sampleExpenses);

        BigDecimal avg = analyticsService.calculateMonthlyAverage(3);

        assertEquals(new BigDecimal("150.00"), avg);
    }

    @Test
    void testCalculateMonthlyAverageZeroMonths() {
        BigDecimal avg = analyticsService.calculateMonthlyAverage(0);
        assertEquals(BigDecimal.ZERO, avg);
    }

    @Test
    void testCalculateTrendIncrease() {
        LocalDate from = LocalDate.of(2024, 3, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);
        LocalDate prevFrom = LocalDate.of(2024, 1, 30);
        LocalDate prevTo = LocalDate.of(2024, 2, 29);

        List<Expense> current = List.of(
                Expense.builder().amount(new BigDecimal("200.00")).aiCategory("Food").expenseDate(from.plusDays(5).atStartOfDay()).build()
        );
        List<Expense> previous = List.of(
                Expense.builder().amount(new BigDecimal("100.00")).aiCategory("Food").expenseDate(prevFrom.plusDays(5).atStartOfDay()).build()
        );

        when(repository.findByDateBetween(from, to)).thenReturn(current);
        when(repository.findByDateBetween(prevFrom, prevTo)).thenReturn(previous);

        String trend = analyticsService.calculateTrend("Food", from, to);

        assertTrue(trend.contains("increased"));
        assertTrue(trend.contains("100.00"));
    }

    @Test
    void testCalculateTrendDecrease() {
        LocalDate from = LocalDate.of(2024, 3, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);
        LocalDate prevFrom = LocalDate.of(2024, 1, 30);
        LocalDate prevTo = LocalDate.of(2024, 2, 29);

        List<Expense> current = List.of(
                Expense.builder().amount(new BigDecimal("50.00")).aiCategory("Food").expenseDate(from.plusDays(5).atStartOfDay()).build()
        );
        List<Expense> previous = List.of(
                Expense.builder().amount(new BigDecimal("100.00")).aiCategory("Food").expenseDate(prevFrom.plusDays(5).atStartOfDay()).build()
        );

        when(repository.findByDateBetween(from, to)).thenReturn(current);
        when(repository.findByDateBetween(prevFrom, prevTo)).thenReturn(previous);

        String trend = analyticsService.calculateTrend("Food", from, to);

        assertTrue(trend.contains("decreased"));
        assertTrue(trend.contains("50.00"));
    }

    @Test
    void testCalculateTrendNoPreviousData() {
        LocalDate from = LocalDate.of(2024, 3, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);
        LocalDate prevFrom = LocalDate.of(2024, 1, 30);
        LocalDate prevTo = LocalDate.of(2024, 2, 29);

        List<Expense> current = List.of(
                Expense.builder().amount(new BigDecimal("200.00")).aiCategory("Food").expenseDate(from.plusDays(5).atStartOfDay()).build()
        );

        when(repository.findByDateBetween(from, to)).thenReturn(current);
        when(repository.findByDateBetween(prevFrom, prevTo)).thenReturn(List.of());

        String trend = analyticsService.calculateTrend("Food", from, to);

        assertTrue(trend.contains("No data for the previous period"));
    }
}
