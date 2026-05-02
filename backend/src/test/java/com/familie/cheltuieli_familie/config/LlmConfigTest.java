package com.familie.cheltuieli_familie.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LlmConfigTest {

    @Test
    void testResolveKeyWithSpringValue() {
        LlmConfig config = new LlmConfig();
        String result = ReflectionTestUtils.invokeMethod(config, "resolveKey", "spring-key", "ENV_VAR");
        assertEquals("spring-key", result);
    }

    @Test
    void testResolveKeyWithEmptySpringValue() {
        LlmConfig config = new LlmConfig();
        String result = ReflectionTestUtils.invokeMethod(config, "resolveKey", "", "PATH");
        // Should fall through to env or .env
        assertNotNull(result);
    }

    @Test
    void testLoadDotEnvReturnsMap() {
        LlmConfig config = new LlmConfig();
        Map<String, String> result = ReflectionTestUtils.invokeMethod(config, "loadDotEnv");
        assertNotNull(result);
    }

    @Test
    void testDeepseekModelWithTestKey() {
        LlmConfig config = new LlmConfig();
        ReflectionTestUtils.setField(config, "deepseekApiKey", "test-key");
        
        var model = config.deepseekModel();
        assertNotNull(model);
    }

    @Test
    void testDeepseekModelWithOpenRouterFallback() {
        LlmConfig config = new LlmConfig();
        ReflectionTestUtils.setField(config, "deepseekApiKey", "");
        ReflectionTestUtils.setField(config, "openRouterApiKey", "test-key");
        
        var model = config.deepseekModel();
        assertNotNull(model);
    }

    @Test
    void testRetrievalAugmentorBean() {
        LlmConfig config = new LlmConfig();
        var retriever = new com.familie.cheltuieli_familie.service.QdrantContentRetriever(null);
        var augmentor = config.retrievalAugmentor(retriever);
        assertNotNull(augmentor);
    }

    @Test
    void testRouterAssistantBean() {
        LlmConfig config = new LlmConfig();
        var mockModel = org.mockito.Mockito.mock(dev.langchain4j.model.chat.ChatLanguageModel.class);
        var assistant = config.routerAssistant(mockModel);
        assertNotNull(assistant);
    }

    @Test
    void testAnalyticsAssistantBean() {
        LlmConfig config = new LlmConfig();
        var mockModel = org.mockito.Mockito.mock(dev.langchain4j.model.chat.ChatLanguageModel.class);
        var mockTools = org.mockito.Mockito.mock(com.familie.cheltuieli_familie.service.ExpenseTools.class);
        var assistant = config.analyticsAssistant(mockModel, mockTools);
        assertNotNull(assistant);
    }

    @Test
    void testReportAssistantBean() {
        LlmConfig config = new LlmConfig();
        var mockModel = org.mockito.Mockito.mock(dev.langchain4j.model.chat.ChatLanguageModel.class);
        var assistant = config.reportAssistant(mockModel);
        assertNotNull(assistant);
    }
}
