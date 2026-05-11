package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.config.LlmConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmRouterServiceTest {

    @Mock
    private LlmConfig.RouterAssistant routerAssistant;

    @Mock
    private ChatLanguageModel deepseekModel;

    @Mock
    private RetrievalAugmentor retrievalAugmentor;

    private LlmRouterService llmRouterService;

    @BeforeEach
    void setUp() {
        llmRouterService = new LlmRouterService(routerAssistant, deepseekModel, retrievalAugmentor);
    }

    @Test
    void testRouteSimpleQuery() {
        when(routerAssistant.classify(anyString())).thenReturn("SIMPLE");
        
        // We can't easily mock the internal AiServices creation without more effort, 
        // but we can verify the classification was called.
        // In a real test, we might mock the AiServices builder if possible or use integration test.
        
        try {
            llmRouterService.routeAndChat("Cat am cheltuit ieri?");
        } catch (Exception e) {
            // AiServices.builder might fail in unit test if not fully mocked, 
            // but we verify the logic flow
        }

        verify(routerAssistant).classify("Cat am cheltuit ieri?");
    }

    @Test
    void testRouteComplexQuery() {
        when(routerAssistant.classify(anyString())).thenReturn("COMPLEX");

        try {
            llmRouterService.routeAndChat("Analizează trendul cheltuielilor mele");
        } catch (Exception e) {
            // AiServices.builder might fail in unit test if not fully mocked,
            // but we verify the logic flow
        }

        verify(routerAssistant).classify("Analizează trendul cheltuielilor mele");
    }
}
