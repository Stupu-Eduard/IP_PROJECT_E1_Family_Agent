package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.config.LlmConfig;
import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagRetrievalServiceTest {

    @Mock
    private QdrantVectorService qdrantVectorService;

    @Mock
    private LlmConfig.RagAssistant ragAssistant;

    @Mock
    private ExpenseJpaRepository expenseJpaRepository;

    @InjectMocks
    private RagRetrievalService ragRetrievalService;

    @Test
    void askWithContext_shouldDelegateToRagAssistant() {
        String query = "How much did I spend?";
        String expectedAnswer = "You spent 500 RON.";

        when(ragAssistant.chat(query)).thenReturn(expectedAnswer);

        String result = ragRetrievalService.askWithContext(query);

        assertEquals(expectedAnswer, result);
        verify(ragAssistant).chat(query);
        verifyNoInteractions(qdrantVectorService);
    }

    @Test
    void retrieveContext_shouldReturnNoResultsMessage_whenEmpty() {
        String query = "Find something";
        when(qdrantVectorService.searchSimilar(query, 10)).thenReturn(Collections.emptyList());

        String result = ragRetrievalService.retrieveContext(query, 10);

        assertEquals("Nu s-au găsit cheltuieli relevante în baza de date.", result);
    }

    @Test
    void retrieveContext_shouldFormatTopResults() {
        String query = "Food expenses";
        List<EmbeddedExpense> results = List.of(
                EmbeddedExpense.builder()
                        .category("food")
                        .amount(new BigDecimal("100.50"))
                        .location("Lidl")
                        .date(LocalDate.of(2024, 1, 15))
                        .person("Teodor")
                        .score(0.95)
                        .build(),
                EmbeddedExpense.builder()
                        .category("food")
                        .amount(new BigDecimal("50.00"))
                        .location(null)
                        .date(null)
                        .person(null)
                        .score(0.85)
                        .build()
        );

        when(qdrantVectorService.searchSimilar(query, 10)).thenReturn(results);

        String result = ragRetrievalService.retrieveContext(query, 10);

        assertTrue(result.contains("Cheltuieli anterioare relevante:"));
        assertTrue(result.contains("Lidl"));
        assertTrue(result.contains("100.50"));
        assertTrue(result.contains("Teodor"));
        assertTrue(result.contains("Necunoscut"));
        assertTrue(result.contains("0.9500"));
    }

    @Test
    void retrieveContext_shouldLimitToTop5Results() {
        String query = "Many expenses";
        List<EmbeddedExpense> results = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> EmbeddedExpense.builder()
                        .category("cat" + i)
                        .amount(new BigDecimal(i))
                        .location("loc" + i)
                        .date(LocalDate.now())
                        .person("person" + i)
                        .score(0.9 - (i * 0.01))
                        .build())
                .toList();

        when(qdrantVectorService.searchSimilar(query, 10)).thenReturn(results);

        String result = ragRetrievalService.retrieveContext(query, 10);

        // Should contain exactly 5 items (numbered 1-5)
        long count = result.lines().filter(line -> line.matches("^\\d+\\..*")).count();
        assertEquals(5, count);
    }

    @Test
    void retrieveContext_shouldSortByScoreDescending() {
        String query = "Sorted expenses";
        List<EmbeddedExpense> results = List.of(
                EmbeddedExpense.builder().category("low").score(0.5).amount(BigDecimal.ONE).build(),
                EmbeddedExpense.builder().category("high").score(0.99).amount(BigDecimal.TEN).build(),
                EmbeddedExpense.builder().category("mid").score(0.75).amount(BigDecimal.ZERO).build()
        );

        when(qdrantVectorService.searchSimilar(query, 10)).thenReturn(results);

        String result = ragRetrievalService.retrieveContext(query, 10);

        // First result should be the highest score (0.99)
        assertTrue(result.indexOf("high") < result.indexOf("mid"));
        assertTrue(result.indexOf("mid") < result.indexOf("low"));
    }

    @Test
    void askWithHybridContext_shouldEnrichQueryWithSemanticAndDbContext() {
        String query = "How much did I spend on food?";

        List<EmbeddedExpense> semanticResults = List.of(
                EmbeddedExpense.builder()
                        .id(1L)
                        .category("food")
                        .amount(new BigDecimal("100.00"))
                        .date(LocalDate.of(2024, 1, 15))
                        .score(0.95)
                        .build()
        );

        List<ExpenseEntity> dbRecords = List.of(
                ExpenseEntity.builder()
                        .id(1L)
                        .category("food")
                        .amount(new BigDecimal("100.00"))
                        .date(LocalDate.of(2024, 1, 15))
                        .person("Teodor")
                        .build()
        );

        when(qdrantVectorService.searchSimilar(query, 10)).thenReturn(semanticResults);
        when(expenseJpaRepository.findAllById(List.of(1L))).thenReturn(dbRecords);
        when(ragAssistant.chat(anyString())).thenReturn("You spent 100 RON on food.");

        String result = ragRetrievalService.askWithHybridContext(query);

        assertEquals("You spent 100 RON on food.", result);
        verify(qdrantVectorService).searchSimilar(query, 10);
        verify(expenseJpaRepository).findAllById(List.of(1L));
        verify(ragAssistant).chat(argThat(arg -> arg.contains("Rezultate semantice din Qdrant") && arg.contains("Date exacte din baza de date")));
    }

    @Test
    void askWithHybridContext_shouldHandleEmptySemanticResults() {
        String query = "Unknown expense";

        when(qdrantVectorService.searchSimilar(query, 10)).thenReturn(Collections.emptyList());
        when(expenseJpaRepository.findAllById(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(ragAssistant.chat(anyString())).thenReturn("No relevant expenses found.");

        String result = ragRetrievalService.askWithHybridContext(query);

        assertEquals("No relevant expenses found.", result);
        verify(ragAssistant).chat(argThat(arg -> arg.contains("Rezultate semantice din Qdrant") && arg.contains("Date exacte din baza de date")));
    }
}
