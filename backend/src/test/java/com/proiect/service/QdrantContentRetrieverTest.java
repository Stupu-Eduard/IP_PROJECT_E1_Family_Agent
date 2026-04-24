package com.proiect.service;

import com.proiect.dto.EmbeddedExpense;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    private ScoringModel scoringModel;

    @InjectMocks
    private QdrantContentRetriever contentRetriever;

    @Test
    void testRetrieveWithResults() {
        EmbeddedExpense expense = EmbeddedExpense.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .rawInput("Am platit 100 lei la Kaufland")
                .build();

        when(qdrantVectorService.searchSimilar(anyString(), anyInt())).thenReturn(List.of(expense));
        when(scoringModel.scoreAll(anyList(), anyString())).thenReturn(Response.from(List.of(0.9)));

        List<Content> results = contentRetriever.retrieve(Query.from("mancare"));

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).textSegment().text().contains("Kaufland"));
    }

    @Test
    void testRetrieveWithNoResults() {
        when(qdrantVectorService.searchSimilar(anyString(), anyInt())).thenReturn(Collections.emptyList());

        List<Content> results = contentRetriever.retrieve(Query.from("vacanta"));

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
