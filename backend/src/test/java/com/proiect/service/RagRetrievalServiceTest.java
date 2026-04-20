package com.proiect.service;

import com.proiect.config.LlmConfig;
import com.proiect.dto.EmbeddedExpense;
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

    @InjectMocks
    private RagRetrievalService ragRetrievalService;

    @Test
    void testAskWithContext() {
        when(ragAssistant.chat("Cât am cheltuit?")).thenReturn("Ai cheltuit 100 RON.");

        String result = ragRetrievalService.askWithContext("Cât am cheltuit?");

        assertEquals("Ai cheltuit 100 RON.", result);
        verify(ragAssistant, times(1)).chat("Cât am cheltuit?");
    }

    @Test
    void testRetrieveContextWithResults() {
        EmbeddedExpense expense = EmbeddedExpense.builder()
                .id(1L)
                .amount(new BigDecimal("150.00"))
                .category("Mâncare")
                .location("Kaufland")
                .date(LocalDate.of(2024, 3, 15))
                .person("Familie")
                .rawInput("Am platit 150 lei la Kaufland")
                .score(0.95)
                .build();

        when(qdrantVectorService.searchSimilar("mancare", 3)).thenReturn(List.of(expense));

        String context = ragRetrievalService.retrieveContext("mancare", 3);

        assertNotNull(context);
        assertTrue(context.contains("Cheltuieli anterioare relevante"));
        assertTrue(context.contains("Mâncare"));
        assertTrue(context.contains("150.00 RON"));
        assertTrue(context.contains("Kaufland"));
    }

    @Test
    void testRetrieveContextWithNoResults() {
        when(qdrantVectorService.searchSimilar("vacanta", 3)).thenReturn(Collections.emptyList());

        String context = ragRetrievalService.retrieveContext("vacanta", 3);

        assertEquals("Nu s-au găsit cheltuieli relevante în baza de date.", context);
    }
}
