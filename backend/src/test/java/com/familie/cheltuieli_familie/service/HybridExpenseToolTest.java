package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HybridExpenseToolTest {

    @Mock
    private QdrantVectorService qdrantVectorService;

    @Mock
    private ExpenseJpaRepository expenseJpaRepository;

    @InjectMocks
    private HybridExpenseTool hybridExpenseTool;

    @Test
    void searchSimilarAndAggregate_shouldReturnRomanianSummary_whenResultsFound() {
        List<EmbeddedExpense> semanticResults = List.of(
                EmbeddedExpense.builder().id(1L).category("food").amount(new BigDecimal("50.00")).score(0.95).build(),
                EmbeddedExpense.builder().id(2L).category("food").amount(new BigDecimal("30.00")).score(0.90).build()
        );

        List<Expense> dbRecords = List.of(
                Expense.builder().id(1L).amount(new BigDecimal("50.00")).expenseDate(LocalDate.of(2024, 1, 10).atStartOfDay()).build(),
                Expense.builder().id(2L).amount(new BigDecimal("30.00")).expenseDate(LocalDate.of(2024, 1, 15).atStartOfDay()).build()
        );

        when(qdrantVectorService.searchSimilar("mancare", 20)).thenReturn(semanticResults);
        when(expenseJpaRepository.findAllById(List.of(1L, 2L))).thenReturn(dbRecords);

        String result = hybridExpenseTool.searchSimilarAndAggregate("mancare", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("S-au găsit 2 cheltuieli similare"));
        assertTrue(result.contains("Total în perioada 2024-01-01–2024-01-31: 80.00 RON"));
        assertTrue(result.contains("(2 cheltuieli)"));
    }

    @Test
    void searchSimilarAndAggregate_shouldFilterByDateRange() {
        List<EmbeddedExpense> semanticResults = List.of(
                EmbeddedExpense.builder().id(1L).category("food").amount(new BigDecimal("50.00")).score(0.95).build(),
                EmbeddedExpense.builder().id(2L).category("food").amount(new BigDecimal("30.00")).score(0.90).build()
        );

        List<Expense> dbRecords = List.of(
                Expense.builder().id(1L).amount(new BigDecimal("50.00")).expenseDate(LocalDate.of(2024, 2, 10).atStartOfDay()).build(),
                Expense.builder().id(2L).amount(new BigDecimal("30.00")).expenseDate(LocalDate.of(2024, 1, 15).atStartOfDay()).build()
        );

        when(qdrantVectorService.searchSimilar("mancare", 20)).thenReturn(semanticResults);
        when(expenseJpaRepository.findAllById(List.of(1L, 2L))).thenReturn(dbRecords);

        String result = hybridExpenseTool.searchSimilarAndAggregate("mancare", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("Total în perioada 2024-01-01–2024-01-31: 30.00 RON"));
        assertTrue(result.contains("(1 cheltuieli)"));
    }

    @Test
    void searchSimilarAndAggregate_shouldReturnNoResultsMessage_whenEmpty() {
        when(qdrantVectorService.searchSimilar("vacanta", 20)).thenReturn(List.of());

        String result = hybridExpenseTool.searchSimilarAndAggregate("vacanta", "2024-01-01", "2024-01-31");

        assertEquals("Nu s-au găsit cheltuieli similare semantic.", result);
    }

    @Test
    void searchSimilarAndAggregate_shouldIgnoreNullIds() {
        List<EmbeddedExpense> semanticResults = List.of(
                EmbeddedExpense.builder().id(null).category("food").amount(new BigDecimal("50.00")).score(0.95).build(),
                EmbeddedExpense.builder().id(2L).category("food").amount(new BigDecimal("30.00")).score(0.90).build()
        );

        List<Expense> dbRecords = List.of(
                Expense.builder().id(2L).amount(new BigDecimal("30.00")).expenseDate(LocalDate.of(2024, 1, 15).atStartOfDay()).build()
        );

        when(qdrantVectorService.searchSimilar("mancare", 20)).thenReturn(semanticResults);
        when(expenseJpaRepository.findAllById(List.of(2L))).thenReturn(dbRecords);

        String result = hybridExpenseTool.searchSimilarAndAggregate("mancare", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("S-au găsit 1 cheltuieli similare"));
        assertTrue(result.contains("30.00 RON"));
    }

    @Test
    void compareSemanticVsDbTotal_shouldReturnComparison() {
        List<EmbeddedExpense> semanticResults = List.of(
                EmbeddedExpense.builder().id(1L).category("food").amount(new BigDecimal("50.00")).score(0.95).build()
        );

        List<Expense> dbRecords = List.of(
                Expense.builder().id(1L).amount(new BigDecimal("50.00")).expenseDate(LocalDate.of(2024, 1, 10).atStartOfDay()).build()
        );

        List<Expense> allInRange = List.of(
                Expense.builder().id(1L).amount(new BigDecimal("50.00")).expenseDate(LocalDate.of(2024, 1, 10).atStartOfDay()).build(),
                Expense.builder().id(3L).amount(new BigDecimal("100.00")).expenseDate(LocalDate.of(2024, 1, 20).atStartOfDay()).build()
        );

        when(qdrantVectorService.searchSimilar("mancare", 20)).thenReturn(semanticResults);
        when(expenseJpaRepository.findAllById(List.of(1L))).thenReturn(dbRecords);
        when(expenseJpaRepository.findByDateBetween(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
                .thenReturn(allInRange);

        String result = hybridExpenseTool.compareSemanticVsDbTotal("mancare", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("total semantic (cheltuieli similare) = 50.00 RON"));
        assertTrue(result.contains("total exact din bază de date = 150.00 RON"));
    }

    @Test
    void compareSemanticVsDbTotal_shouldHandleEmptySemanticResults() {
        when(qdrantVectorService.searchSimilar("vacanta", 20)).thenReturn(List.of());

        List<Expense> allInRange = List.of(
                Expense.builder().id(3L).amount(new BigDecimal("200.00")).expenseDate(LocalDate.of(2024, 1, 20).atStartOfDay()).build()
        );

        when(expenseJpaRepository.findByDateBetween(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
                .thenReturn(allInRange);

        String result = hybridExpenseTool.compareSemanticVsDbTotal("vacanta", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("total semantic (cheltuieli similare) = 0 RON"));
        assertTrue(result.contains("total exact din bază de date = 200.00 RON"));
    }
}
