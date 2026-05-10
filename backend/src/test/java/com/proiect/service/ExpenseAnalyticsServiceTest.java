package com.proiect.service;
import org.springframework.test.context.ContextConfiguration;

import com.proiect.model.ExpenseEntity;
import com.proiect.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = com.familie.cheltuieli_familie.CheltuieliFamilieApplication.class)
class ExpenseAnalyticsServiceTest {

    @Mock
    private ExpenseJpaRepository repository;

    @InjectMocks
    private ExpenseAnalyticsService analyticsService;

    private List<ExpenseEntity> sampleExpenses;

    @BeforeEach
    void setUp() {
        sampleExpenses = List.of(
                ExpenseEntity.builder().amount(new BigDecimal("100.00")).category("Food").person("Alice").date(LocalDate.now()).build(),
                ExpenseEntity.builder().amount(new BigDecimal("200.00")).category("Transport").person("Bob").date(LocalDate.now()).build(),
                ExpenseEntity.builder().amount(new BigDecimal("150.00")).category("Food").person("Alice").date(LocalDate.now()).build()
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

        List<ExpenseEntity> anomalies = analyticsService.detectAnomalies(new BigDecimal("180.00"));

        assertEquals(1, anomalies.size());
        assertEquals(new BigDecimal("200.00"), anomalies.get(0).getAmount());
    }
}
