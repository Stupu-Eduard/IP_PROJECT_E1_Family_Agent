package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
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
class QdrantContentRetrieverTest {

    @Mock
    private QdrantVectorService qdrantVectorService;

    @InjectMocks
    private QdrantContentRetriever contentRetriever;

    @Test
    void testRetrieveWithResults() {
        EmbeddedExpense expense = EmbeddedExpense.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .rawInput("Am platit 100 lei la Kaufland")
                .score(0.9)
                .build();

        when(qdrantVectorService.searchSimilar(anyString(), anyInt())).thenReturn(List.of(expense));

        List<Content> results = contentRetriever.retrieve(Query.from("mancare"));

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).textSegment().text().contains("Kaufland"));
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

        when(qdrantVectorService.searchSimilar(anyString(), anyInt())).thenReturn(List.of(expense));

        List<Content> results = contentRetriever.retrieve(Query.from("food"));

        assertNotNull(results);
        assertEquals(1, results.size());
        String text = results.get(0).textSegment().text();
        assertTrue(text.contains("Lidl"));
        assertTrue(text.contains("Food"));
    }

    @Test
    void testRetrieveWithNoResults() {
        when(qdrantVectorService.searchSimilar(anyString(), anyInt())).thenReturn(Collections.emptyList());

        List<Content> results = contentRetriever.retrieve(Query.from("vacanta"));

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testRetrieveLimitsToTop5() {
        List<EmbeddedExpense> expenses = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> EmbeddedExpense.builder()
                        .id((long) i)
                        .rawInput("Expense " + i)
                        .score(0.5 + i * 0.05)
                        .build())
                .toList();

        when(qdrantVectorService.searchSimilar(anyString(), anyInt())).thenReturn(expenses);

        List<Content> results = contentRetriever.retrieve(Query.from("query"));

        assertNotNull(results);
        assertEquals(5, results.size());
    }
}
