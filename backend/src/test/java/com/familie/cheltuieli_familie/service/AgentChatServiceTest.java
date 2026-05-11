package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.response.ChartPayload;
import com.familie.cheltuieli_familie.dto.response.ChartResponseDTO;
import com.familie.cheltuieli_familie.dto.response.TextResponseDTO;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void processQuery_shouldReturnChartResponse_whenIntentIsChart() {
        String userMessage = "Show me expenses by category";
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .responseType("chart")
                .chartType("bar")
                .groupBy("category")
                .build();
        ChartResponseDTO chartResponse = new ChartResponseDTO("Here are your expenses",
                ChartPayload.builder().chartType("bar").build());

        when(visualIntentExtractor.extract(userMessage)).thenReturn(intent);
        when(chartGenerationService.generate(intent)).thenReturn(chartResponse);

        var result = agentChatService.processQuery(userMessage);

        assertInstanceOf(ChartResponseDTO.class, result);
        assertEquals("chart", result.getType());
        verify(chartGenerationService).generate(intent);
        verifyNoInteractions(ragRetrievalService);
    }

    @Test
    void processQuery_shouldReturnTextResponse_whenIntentIsText() {
        String userMessage = "How much did I spend last month?";
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .responseType("text")
                .build();
        String ragAnswer = "You spent 500 RON last month.";

        when(visualIntentExtractor.extract(userMessage)).thenReturn(intent);
        when(ragRetrievalService.askWithContext(userMessage)).thenReturn(ragAnswer);

        var result = agentChatService.processQuery(userMessage);

        assertInstanceOf(TextResponseDTO.class, result);
        assertEquals("text", result.getType());
        assertEquals(ragAnswer, result.getMessage());
        verify(ragRetrievalService).askWithContext(userMessage);
        verifyNoInteractions(chartGenerationService);
    }

    @Test
    void processQuery_shouldFallbackToText_whenChartGenerationFails() {
        String userMessage = "Show me expenses";
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .responseType("chart")
                .chartType("pie")
                .build();
        String ragAnswer = "Fallback answer";

        when(visualIntentExtractor.extract(userMessage)).thenReturn(intent);
        when(chartGenerationService.generate(intent)).thenThrow(new RuntimeException("Chart failed"));
        when(ragRetrievalService.askWithContext(userMessage)).thenReturn(ragAnswer);

        var result = agentChatService.processQuery(userMessage);

        assertInstanceOf(TextResponseDTO.class, result);
        assertEquals(ragAnswer, result.getMessage());
        verify(ragRetrievalService).askWithContext(userMessage);
    }

    @Test
    void processQuery_shouldFallbackToText_whenIntentExtractionFails() {
        String userMessage = "Some query";
        String ragAnswer = "Fallback answer";

        when(visualIntentExtractor.extract(userMessage)).thenThrow(new RuntimeException("Extraction failed"));
        when(ragRetrievalService.askWithContext(userMessage)).thenReturn(ragAnswer);

        var result = agentChatService.processQuery(userMessage);

        assertInstanceOf(TextResponseDTO.class, result);
        assertEquals(ragAnswer, result.getMessage());
    }

    @Test
    void processQuery_shouldHandleNullResponseTypeAsText() {
        String userMessage = "What is this?";
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .responseType(null)
                .build();
        String ragAnswer = "Answer";

        when(visualIntentExtractor.extract(userMessage)).thenReturn(intent);
        when(ragRetrievalService.askWithContext(userMessage)).thenReturn(ragAnswer);

        var result = agentChatService.processQuery(userMessage);

        assertInstanceOf(TextResponseDTO.class, result);
    }
}
