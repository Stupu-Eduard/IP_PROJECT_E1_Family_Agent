package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.security.util.SecurityService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagRetrievalServiceTest {

    @Mock
    private QdrantVectorService qdrantVectorService;

    @Mock
    private LlmRouterService llmRouterService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private RagRetrievalService ragRetrievalService;

    @Test
    void askWithContext_shouldDelegateAugmentedQueryToLlmRouterService() {
        String query = "How much did I spend?";
        String expectedAnswer = "You spent 500 RON.";

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(llmRouterService.routeAndChat(anyString())).thenReturn(expectedAnswer);
        when(qdrantVectorService.searchSimilar(anyString(), anyInt(), any(), any())).thenReturn(Collections.emptyList());

        String result = ragRetrievalService.askWithContext(query);

        assertEquals(expectedAnswer, result);
        verify(llmRouterService).routeAndChat(contains(query));
        verify(qdrantVectorService).searchSimilar(query, 10, null, 1L);
    }

    @Test
    void retrieveContext_shouldReturnNoResultsMessage_whenEmpty() {
        String query = "Find something";
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(query, 10, null, 1L)).thenReturn(Collections.emptyList());

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

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(query, 10, null, 1L)).thenReturn(results);

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

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(query, 10, null, 1L)).thenReturn(results);

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

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(query, 10, null, 1L)).thenReturn(results);

        String result = ragRetrievalService.retrieveContext(query, 10);

        // First result should be the highest score (0.99)
        assertTrue(result.indexOf("high") < result.indexOf("mid"));
        assertTrue(result.indexOf("mid") < result.indexOf("low"));
    }
}
