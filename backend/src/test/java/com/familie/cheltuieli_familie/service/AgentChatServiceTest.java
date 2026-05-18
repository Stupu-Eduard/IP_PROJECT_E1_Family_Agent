package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.response.AgentResponseDTO;
import com.familie.cheltuieli_familie.dto.response.ChartResponseDTO;
import com.familie.cheltuieli_familie.dto.response.TextResponseDTO;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentChatServiceTest {

    @Mock private VisualIntentExtractor visualIntentExtractor;
    @Mock private ChartGenerationService chartGenerationService;
    @Mock private RagRetrievalService ragRetrievalService;
    @Mock private FamilyMemberRepository familyMemberRepository;

    @InjectMocks
    private AgentChatService agentChatService;

    @Test
    void processQuery_shouldHandleChartIntent() {
        ChartQueryIntent intent = new ChartQueryIntent();
        intent.setResponseType("chart");
        when(visualIntentExtractor.extract(anyString())).thenReturn(intent);
        
        ChartResponseDTO mockResponse = mock(ChartResponseDTO.class);
        when(chartGenerationService.generate(intent)).thenReturn(mockResponse);

        AgentResponseDTO result = agentChatService.processQuery("vrem un grafic");

        assertEquals(mockResponse, result);
        verify(chartGenerationService).generate(intent);
    }

    @Test
    void processQuery_shouldFallbackToRag_onException() {
        when(visualIntentExtractor.extract(anyString())).thenThrow(new RuntimeException("Extractor failed"));
        when(ragRetrievalService.askWithContext(anyString())).thenReturn("RAG Answer");

        AgentResponseDTO result = agentChatService.processQuery("buna salut te rog");

        assertTrue(result instanceof TextResponseDTO);
        assertEquals("RAG Answer", ((TextResponseDTO)result).getMessage());
    }

    @Test
    void stripMarkdown_shouldRemoveAllMarkdownFormatting() {
        String markdown = """
            # Header
            ## Subheader
            | col |
            | --- |
            | val |
            **bold** and _italic_
            ```java
            code
            ```
            - list item
            1. numbered item
            """;
        
        String result = AgentChatService.stripMarkdown(markdown);
        
        assertFalse(result.contains("```"));
        assertFalse(result.contains("# Header"));
        assertFalse(result.contains("|"));
        assertFalse(result.contains("**"));
        assertFalse(result.contains("_italic_"));
        assertFalse(result.contains("- list item"));
        assertFalse(result.contains("1. numbered item"));
        assertTrue(result.contains("bold"));
        assertTrue(result.contains("italic"));
        assertTrue(result.contains("list item"));
        assertTrue(result.contains("numbered item"));
    }

    @Test
    void processQuery_withAuthenticatedUser() {
        User user = new User();
        user.setId(1L);
        user.setName("John");
        
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(auth);

        ChartQueryIntent intent = new ChartQueryIntent();
        intent.setResponseType("text");
        when(visualIntentExtractor.extract(anyString())).thenReturn(intent);
        when(ragRetrievalService.askWithContext(contains("John"))).thenReturn("Hello John");

        AgentResponseDTO result = agentChatService.processQuery("hi");

        assertEquals("Hello John", ((TextResponseDTO)result).getMessage());
        
        SecurityContextHolder.clearContext();
    }
}
