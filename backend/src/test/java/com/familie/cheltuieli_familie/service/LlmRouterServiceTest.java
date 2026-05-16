package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.config.LlmConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmRouterServiceTest {

    @Mock
    private LlmConfig.RouterAssistant routerAssistant;

    @Mock
    private LlmConfig.RagAssistant ragAssistant;

    @InjectMocks
    private LlmRouterService llmRouterService;

    @Test
    void routeAndChat_shouldReturnResponse_whenAllValid() {
        when(ragAssistant.chat("hello")).thenReturn("Hi there");

        String result = llmRouterService.routeAndChat("hello");
        assertEquals("Hi there", result);
    }

    @Test
    void routeAndChat_shouldReturnNull_whenChatReturnsNull() {
        when(ragAssistant.chat("hello")).thenReturn(null);

        String result = llmRouterService.routeAndChat("hello");
        assertNull(result);
    }

    @Test
    void routeAndChat_shouldReturnBlank_whenChatReturnsBlank() {
        when(ragAssistant.chat("hello")).thenReturn("   ");

        String result = llmRouterService.routeAndChat("hello");
        assertEquals("   ", result);
    }
}
