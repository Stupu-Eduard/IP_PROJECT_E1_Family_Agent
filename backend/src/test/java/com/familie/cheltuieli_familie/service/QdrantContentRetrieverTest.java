package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.security.util.SecurityService;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QdrantContentRetrieverTest {

    @Mock
    private QdrantVectorService qdrantVectorService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private QdrantContentRetriever contentRetriever;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contentRetriever, "minScoreThreshold", 0.35);
        ReflectionTestUtils.setField(contentRetriever, "shortQueryThreshold", 0.22);
    }

    @Test
    void testRetrieveWithResults() {
        EmbeddedExpense expense = EmbeddedExpense.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .rawInput("Am platit 100 lei la Kaufland")
                .score(0.9)
                .build();

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(anyString(), anyInt(), any(), any())).thenReturn(List.of(expense));

        List<Content> results = contentRetriever.retrieve(Query.from("mancare"));

        assertNotNull(results);
        assertEquals(1, results.size());
        String text = results.get(0).textSegment().text();
        assertTrue(text.contains("Kaufland"));
        assertTrue(text.startsWith("[RAG_CONTEXT] "));
    }

    @Test
    void testRetrieveWithNullRawInput() {
        EmbeddedExpense expense = EmbeddedExpense.builder()
                .id(2L)
                .amount(new BigDecimal("50.00"))
                .category("Food")
                .location("Lidl")
                .date(LocalDate.of(2024, 1, 15))
                .rawInput(null)
                .score(0.85)
                .build();

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(anyString(), anyInt(), any(), any())).thenReturn(List.of(expense));

        List<Content> results = contentRetriever.retrieve(Query.from("food"));

        assertNotNull(results);
        assertEquals(1, results.size());
        String text = results.get(0).textSegment().text();
        assertTrue(text.contains("Lidl"));
        assertTrue(text.contains("Food"));
        assertTrue(text.startsWith("[RAG_CONTEXT] "));
    }

    @Test
    void testRetrieveWithNoResults() {
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(anyString(), anyInt(), any(), any())).thenReturn(Collections.emptyList());

        List<Content> results = contentRetriever.retrieve(Query.from("vacanta"));

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testRetrieveLimitsToTop10() {
        List<EmbeddedExpense> expenses = java.util.stream.IntStream.range(0, 15)
                .mapToObj(i -> EmbeddedExpense.builder()
                        .id((long) i)
                        .rawInput("Expense " + i)
                        .score(0.5 + i * 0.05)
                        .build())
                .toList();

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(anyString(), anyInt(), any(), any())).thenReturn(expenses);

        List<Content> results = contentRetriever.retrieve(Query.from("query"));

        assertNotNull(results);
        assertEquals(10, results.size());
    }

    @Test
    void testRetrieveFiltersByScoreThreshold() {
        EmbeddedExpense highScore = EmbeddedExpense.builder()
                .id(1L)
                .rawInput("High score")
                .score(0.9)
                .build();
        EmbeddedExpense lowScore = EmbeddedExpense.builder()
                .id(2L)
                .rawInput("Low score")
                .score(0.1)
                .build();

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(anyString(), anyInt(), any(), any())).thenReturn(List.of(highScore, lowScore));

        List<Content> results = contentRetriever.retrieve(Query.from("query"));

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).textSegment().text().contains("High score"));
    }

    @Test
    void testRetrieveWithStopWordsAndQuotes() {
        EmbeddedExpense expense = EmbeddedExpense.builder()
                .id(1L)
                .rawInput("Test expense")
                .score(0.95)
                .build();

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(anyString(), anyInt(), any(), any())).thenReturn(List.of(expense));

        List<Content> results = contentRetriever.retrieve(Query.from("'\"mancare'\""));

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(qdrantVectorService).searchSimilar(argThat(arg -> arg != null && !arg.contains("'") && !arg.contains("\"")), anyInt(), any(), any());
    }

    @Test
    void testRetrieveStripsStopWords() {
        EmbeddedExpense expense = EmbeddedExpense.builder()
                .id(1L)
                .rawInput("Test expense")
                .score(0.95)
                .build();

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(anyString(), anyInt(), any(), any())).thenReturn(List.of(expense));

        List<Content> results = contentRetriever.retrieve(Query.from("salut buna te rog mancare"));

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(qdrantVectorService).searchSimilar(argThat(arg -> arg != null && !arg.toLowerCase().contains("salut")), anyInt(), any(), any());
    }
}
