package com.familie.cheltuieli_familie.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingConfigTest {

    @Test
    void testResolveKeyWithSpringValue() {
        EmbeddingConfig config = new EmbeddingConfig();
        String result = ReflectionTestUtils.invokeMethod(config, "resolveKey", "spring-key", "ENV_VAR");
        assertEquals("spring-key", result);
    }

    @Test
    void testLoadDotEnvReturnsMap() {
        EmbeddingConfig config = new EmbeddingConfig();
        Map<String, String> result = ReflectionTestUtils.invokeMethod(config, "loadDotEnv");
        assertNotNull(result);
    }

    @Test
    void testEmbeddingModelWithOpenRouterKey() {
        EmbeddingConfig config = new EmbeddingConfig();
        ReflectionTestUtils.setField(config, "openRouterApiKey", "test-key");
        
        var model = config.embeddingModel();
        assertNotNull(model);
    }
}
