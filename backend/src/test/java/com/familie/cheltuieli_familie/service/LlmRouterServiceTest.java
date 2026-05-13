package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.config.LlmConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmRouterServiceTest {

    @Mock
    private LlmConfig.RouterAssistant routerAssistant;

    @Mock
    private ChatLanguageModel deepseekModel;

    @Mock
    private RetrievalAugmentor retrievalAugmentor;

    @Mock
    private LlmConfig.RagAssistant ragAssistant;

    @InjectMocks
    private LlmRouterService llmRouterService;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private MockedStatic<AiServices> mockAiServicesStatic() {
        MockedStatic<AiServices> mocked = mockStatic(AiServices.class);
        AiServices builder = mock(AiServices.class);
        when(builder.chatLanguageModel(any(ChatLanguageModel.class))).thenReturn(builder);
        when(builder.retrievalAugmentor(any(RetrievalAugmentor.class))).thenReturn(builder);
        when(builder.build()).thenReturn(ragAssistant);
        mocked.when(() -> AiServices.builder(LlmConfig.RagAssistant.class)).thenReturn(builder);
        return mocked;
    }

    @Test
    void routeAndChat_shouldReturnResponse_whenAllValid() {
        when(routerAssistant.classify("hello")).thenReturn("SIMPLE");
        when(ragAssistant.chat("hello")).thenReturn("Hi there");

        try (MockedStatic<AiServices> mocked = mockAiServicesStatic()) {
            String result = llmRouterService.routeAndChat("hello");
            assertEquals("Hi there", result);
        }
    }

    @Test
    void routeAndChat_shouldDefaultToSimple_whenClassificationIsNull() {
        when(routerAssistant.classify("hello")).thenReturn(null);
        when(ragAssistant.chat("hello")).thenReturn("Hi there");

        try (MockedStatic<AiServices> mocked = mockAiServicesStatic()) {
            String result = llmRouterService.routeAndChat("hello");
            assertEquals("Hi there", result);
        }
    }

    @Test
    void routeAndChat_shouldDefaultToSimple_whenClassificationIsBlank() {
        when(routerAssistant.classify("hello")).thenReturn("   ");
        when(ragAssistant.chat("hello")).thenReturn("Hi there");

        try (MockedStatic<AiServices> mocked = mockAiServicesStatic()) {
            String result = llmRouterService.routeAndChat("hello");
            assertEquals("Hi there", result);
        }
    }

    @Test
    void routeAndChat_shouldReturnNull_whenChatReturnsNull() {
        when(routerAssistant.classify("hello")).thenReturn("SIMPLE");
        when(ragAssistant.chat("hello")).thenReturn(null);

        try (MockedStatic<AiServices> mocked = mockAiServicesStatic()) {
            String result = llmRouterService.routeAndChat("hello");
            assertNull(result);
        }
    }

    @Test
    void routeAndChat_shouldReturnBlank_whenChatReturnsBlank() {
        when(routerAssistant.classify("hello")).thenReturn("SIMPLE");
        when(ragAssistant.chat("hello")).thenReturn("   ");

        try (MockedStatic<AiServices> mocked = mockAiServicesStatic()) {
            String result = llmRouterService.routeAndChat("hello");
            assertEquals("   ", result);
        }
    }
}
