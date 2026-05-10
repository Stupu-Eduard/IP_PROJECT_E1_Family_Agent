package com.proiect.service;
import org.springframework.test.context.ContextConfiguration;

import com.proiect.dto.response.AgentResponseDTO;
import com.proiect.dto.response.ChartPayload;
import com.proiect.dto.response.ChartResponseDTO;
import com.proiect.dto.response.TextResponseDTO;
import com.proiect.model.ChartFilters;
import com.proiect.model.ChartQueryIntent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentChatServiceTest {

    @Mock
    private VisualIntentExtractor visualIntentExtractor;

    @Mock
    private ChartGenerationService chartGenerationService;

    @Mock
    private RagRetrievalService ragRetrievalService;

    @InjectMocks
    private AgentChatService agentChatService;

    @Test
    void testTextQuery() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .responseType("text")
                .build();

        when(visualIntentExtractor.extract("Cât am cheltuit?")).thenReturn(intent);
        when(ragRetrievalService.askWithContext("Cât am cheltuit?")).thenReturn("Ai cheltuit 100 RON.");

        AgentResponseDTO response = agentChatService.processQuery("Cât am cheltuit?");

        assertInstanceOf(TextResponseDTO.class, response);
        assertEquals("text", response.getType());
        assertEquals("Ai cheltuit 100 RON.", response.getMessage());
        verify(chartGenerationService, never()).generate(any(), any());
    }

    @Test
    void testChartQuery() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .responseType("chart")
                .chartType("bar")
                .build();

        ChartPayload payload = ChartPayload.builder()
                .chartType("bar")
                .title("Test")
                .data(List.of(Map.of("name", "A", "value", 100)))
                .dataKeys(List.of("value"))
                .xAxisKey("name")
                .build();

        when(visualIntentExtractor.extract("Compară cheltuielile")).thenReturn(intent);
        when(chartGenerationService.generate("Compară cheltuielile", intent))
                .thenReturn(new ChartResponseDTO("Rezultat:", payload));

        AgentResponseDTO response = agentChatService.processQuery("Compară cheltuielile");

        assertInstanceOf(ChartResponseDTO.class, response);
        assertEquals("chart", response.getType());
        verify(ragRetrievalService, never()).askWithContext(any());
    }

    @Test
    void testFallbackOnException() {
        when(visualIntentExtractor.extract("Eroare")).thenThrow(new RuntimeException("LLM failed"));
        when(ragRetrievalService.askWithContext("Eroare")).thenReturn("Răspuns fallback.");

        AgentResponseDTO response = agentChatService.processQuery("Eroare");

        assertInstanceOf(TextResponseDTO.class, response);
        assertEquals("Răspuns fallback.", response.getMessage());
    }
}
