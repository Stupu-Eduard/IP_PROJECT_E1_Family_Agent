package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.response.ChartPayload;
import com.familie.cheltuieli_familie.dto.response.ChartResponseDTO;
import com.familie.cheltuieli_familie.dto.response.TextResponseDTO;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @Mock
    private com.familie.cheltuieli_familie.repository.FamilyMemberRepository familyMemberRepository;

    @InjectMocks
    private AgentChatService agentChatService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

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

    @Test
    void processQuery_shouldReturnFallbackText_whenRagReturnsNull() {
        String userMessage = "How much did I spend?";
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .responseType("text")
                .build();

        when(visualIntentExtractor.extract(userMessage)).thenReturn(intent);
        when(ragRetrievalService.askWithContext(userMessage)).thenReturn(null);

        var result = agentChatService.processQuery(userMessage);

        assertInstanceOf(TextResponseDTO.class, result);
        assertNotNull(result.getMessage());
        assertFalse(result.getMessage().isBlank());
    }

    @Test
    void processQuery_shouldReturnFallbackText_whenRagReturnsBlank() {
        String userMessage = "How much did I spend?";
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .responseType("text")
                .build();

        when(visualIntentExtractor.extract(userMessage)).thenReturn(intent);
        when(ragRetrievalService.askWithContext(userMessage)).thenReturn("   ");

        var result = agentChatService.processQuery(userMessage);

        assertInstanceOf(TextResponseDTO.class, result);
        assertNotNull(result.getMessage());
        assertFalse(result.getMessage().isBlank());
    }

    @Test
    void processQuery_shouldReturnFallbackText_whenChartFailsAndRagReturnsNull() {
        String userMessage = "Show me expenses";
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .responseType("chart")
                .chartType("pie")
                .build();

        when(visualIntentExtractor.extract(userMessage)).thenReturn(intent);
        when(chartGenerationService.generate(intent)).thenThrow(new RuntimeException("Chart failed"));
        when(ragRetrievalService.askWithContext(userMessage)).thenReturn(null);

        var result = agentChatService.processQuery(userMessage);

        assertInstanceOf(TextResponseDTO.class, result);
        assertNotNull(result.getMessage());
        assertFalse(result.getMessage().isBlank());
    }

    @Test
    void stripMarkdown_shouldReturnNullForNull() {
        assertNull(AgentChatService.stripMarkdown(null));
    }

    @Test
    void stripMarkdown_shouldReturnBlankForBlank() {
        assertEquals("   ", AgentChatService.stripMarkdown("   "));
    }

    @Test
    void stripMarkdown_shouldRemoveTables() {
        String input = "| col1 | col2 |\n| val1 | val2 |\nHello world";
        String result = AgentChatService.stripMarkdown(input);
        assertFalse(result.contains("|"));
        assertTrue(result.contains("Hello world"));
    }

    @Test
    void stripMarkdown_shouldRemoveBoldAndItalic() {
        String input = "This is **bold** and __italic__ and *also* _this_";
        String result = AgentChatService.stripMarkdown(input);
        assertFalse(result.contains("**"));
        assertFalse(result.contains("__"));
        assertTrue(result.contains("bold"));
        assertTrue(result.contains("italic"));
    }

    @Test
    void stripMarkdown_shouldRemoveCodeBlocks() {
        String input = "Some text ```java code``` more text";
        String result = AgentChatService.stripMarkdown(input);
        assertFalse(result.contains("```"));
        assertTrue(result.contains("Some text"));
        assertTrue(result.contains("more text"));
    }

    @Test
    void stripMarkdown_shouldRemoveInlineCode() {
        String input = "Use `variable` here";
        String result = AgentChatService.stripMarkdown(input);
        assertFalse(result.contains("`"));
        assertTrue(result.contains("Use"));
        assertTrue(result.contains("here"));
    }

    @Test
    void stripMarkdown_shouldRemoveHeaders() {
        String input = "# Header 1\n## Header 2\nNormal text";
        String result = AgentChatService.stripMarkdown(input);
        assertFalse(result.contains("#"));
        assertTrue(result.contains("Header 1"));
        assertTrue(result.contains("Normal text"));
    }

    @Test
    void stripMarkdown_shouldRemoveBulletLists() {
        String input = "- item 1\n* item 2\n+ item 3";
        String result = AgentChatService.stripMarkdown(input);
        assertTrue(result.contains("item 1"));
        assertTrue(result.contains("item 2"));
        assertFalse(result.startsWith("-"));
    }

    @Test
    void stripMarkdown_shouldRemoveNumberedLists() {
        String input = "1. First\n2. Second";
        String result = AgentChatService.stripMarkdown(input);
        assertTrue(result.contains("First"));
        assertTrue(result.contains("Second"));
        assertFalse(result.contains("1."));
    }

    @Test
    void stripMarkdown_shouldCollapseMultipleNewlines() {
        String input = "Line 1\n\n\nLine 2";
        String result = AgentChatService.stripMarkdown(input);
        assertFalse(result.contains("\n\n"));
        assertTrue(result.contains("Line 1"));
        assertTrue(result.contains("Line 2"));
    }

    @Test
    void cleanQueryForRag_shouldRemoveStopWords() {
        String input = "salut buna te rog spune-mi despre cheltuieli";
        String result = invokeCleanQueryForRag(input);
        assertFalse(result.contains("salut"));
        assertFalse(result.contains("buna"));
        assertTrue(result.contains("cheltuieli"));
    }

    @Test
    void cleanQueryForRag_shouldStripQuotes() {
        String input = "\"How much did I spend?\"";
        String result = invokeCleanQueryForRag(input);
        assertFalse(result.contains("\""));
        assertTrue(result.contains("How much did I spend?"));
    }

    @Test
    void cleanQueryForRag_shouldReturnNullForNull() {
        assertNull(invokeCleanQueryForRag(null));
    }

    @Test
    void cleanQueryForRag_shouldReturnBlankForBlank() {
        assertEquals("   ", invokeCleanQueryForRag("   "));
    }

    private String invokeCleanQueryForRag(String input) {
        return org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                agentChatService, "cleanQueryForRag", input);
    }
}
