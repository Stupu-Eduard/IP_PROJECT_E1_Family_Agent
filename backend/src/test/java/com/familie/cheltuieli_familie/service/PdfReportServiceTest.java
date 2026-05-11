package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.repository.AlertRepository;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfReportServiceTest {

    @Mock
    private ExpenseJpaRepository expenseRepository;

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private PdfReportService pdfReportService;

    private ExpenseEntity expense;
    private Alert alert;

    @BeforeEach
    void setUp() {
        expense = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .category("Food")
                .location("Kaufland")
                .person("Andrei")
                .date(LocalDate.now())
                .build();

        alert = Alert.builder()
                .id(1L)
                .restrictedCategory("Divertisment")
                .extraCost(new BigDecimal("50.00"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void testGenerateFinancialReportSuccess() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        when(expenseRepository.findByDateBetween(from, to)).thenReturn(List.of(expense));
        when(alertRepository.findByTimestampBetween(any(), any())).thenReturn(List.of(alert));

        byte[] pdfBytes = pdfReportService.generateFinancialReport(from, to);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerateFinancialReportEmptyData() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        when(expenseRepository.findByDateBetween(from, to)).thenReturn(List.of());
        when(alertRepository.findByTimestampBetween(any(), any())).thenReturn(List.of());

        byte[] pdfBytes = pdfReportService.generateFinancialReport(from, to);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerateFinancialReportWithNullValues() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        ExpenseEntity nullExpense = ExpenseEntity.builder().id(2L).build();
        Alert nullAlert = Alert.builder().id(2L).build();

        when(expenseRepository.findByDateBetween(from, to)).thenReturn(List.of(nullExpense));
        when(alertRepository.findByTimestampBetween(any(), any())).thenReturn(List.of(nullAlert));

        byte[] pdfBytes = pdfReportService.generateFinancialReport(from, to);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerateFinancialReportError() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        when(expenseRepository.findByDateBetween(from, to)).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> pdfReportService.generateFinancialReport(from, to));
    }
}
