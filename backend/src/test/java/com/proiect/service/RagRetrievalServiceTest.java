package com.proiect.service;

import com.proiect.dto.EmbeddedExpense;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
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
    private ScoringModel scoringModel;

    @InjectMocks
    private RagRetrievalService ragRetrievalService;

    @Test
    void testAskWithContext() {
        when(llmRouterService.routeAndChat("Cât am cheltuit?")).thenReturn("Ai cheltuit 100 RON.");

        String result = ragRetrievalService.askWithContext("Cât am cheltuit?");

        assertEquals("Ai cheltuit 100 RON.", result);
        verify(llmRouterService, times(1)).routeAndChat("Cât am cheltuit?");
    }

    @Test
    void testRetrieveContextWithResults() {
        EmbeddedExpense expense = EmbeddedExpense.builder()
                .id(1L)
                .amount(new BigDecimal("150.00"))
                .category("Mancare")
                .location("Kaufland")
                .date(LocalDate.of(2024, 3, 15))
                .person("Familie")
                .rawInput("Am platit 150 lei la Kaufland")
                .build();

        when(qdrantVectorService.searchSimilar(anyString(), anyInt())).thenReturn(List.of(expense));
        when(scoringModel.scoreAll(anyList(), anyString())).thenReturn(Response.from(List.of(0.95)));

        String context = ragRetrievalService.retrieveContext("mancare", 3);

        assertNotNull(context);
        assertTrue(context.contains("Cheltuieli anterioare relevante"));
        assertTrue(context.contains("Mancare"));
        assertTrue(context.contains("150"));
        assertTrue(context.contains("RON"));
        assertTrue(context.contains("Kaufland"));
    }

    @Test
    void testRetrieveContextWithNoResults() {
        when(qdrantVectorService.searchSimilar(anyString(), anyInt())).thenReturn(Collections.emptyList());

        String context = ragRetrievalService.retrieveContext("vacanta", 3);

        assertEquals("Nu s-au găsit cheltuieli relevante în baza de date.", context);
    }
}
