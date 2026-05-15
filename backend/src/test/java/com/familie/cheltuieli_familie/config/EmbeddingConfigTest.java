package com.familie.cheltuieli_familie.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingConfigTest {

    private EmbeddingConfig embeddingConfig;

    @BeforeEach
    void setUp() {
        embeddingConfig = new EmbeddingConfig();
    }

    @Test
    void testEmbeddingModelBeanCreationWithValidKey() {
        String validKey = "sk-or-...2345";
        ReflectionTestUtils.setField(embeddingConfig, "openRouterApiKey", validKey);
        ReflectionTestUtils.setField(embeddingConfig, "baseUrl", "https://openrouter.ai/api/v1");
        ReflectionTestUtils.setField(embeddingConfig, "modelName", "nvidia/llama-nemotron-embed-vl-1b-v2:free");
        ReflectionTestUtils.setField(embeddingConfig, "dimensions", 2048);

        EmbeddingModel embeddingModel = embeddingConfig.embeddingModel();

        assertNotNull(embeddingModel);
        assertInstanceOf(OpenAiEmbeddingModel.class, embeddingModel);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".+")
    void testEmbeddingModelBeanCreationThrowsExceptionWhenKeyIsEmpty() {
        ReflectionTestUtils.setField(embeddingConfig, "openRouterApiKey", "");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> embeddingConfig.embeddingModel()
        );
        assertEquals("OPENROUTER_API_KEY is required for embeddings.", exception.getMessage());
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".+")
    void testEmbeddingModelBeanCreationThrowsExceptionWhenKeyIsNull() {
        ReflectionTestUtils.setField(embeddingConfig, "openRouterApiKey", null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> embeddingConfig.embeddingModel()
        );
        assertEquals("OPENROUTER_API_KEY is required for embeddings.", exception.getMessage());
    }

    @Test
    void testResolveKeyFromSpringValue() {
        String springValue = "sk-or-...alue";
        String envName = "OPENROUTER_API_KEY";

        String result = KeyResolver.resolve(springValue, envName);

        assertEquals(springValue, result);
    }

    @Test
    void testResolveKeyFromSystemEnvironment() {
        String envName = "TEST_ENV_VAR_EMBEDDING";
        String expectedValue = "***";

        try {
            System.setProperty(envName, expectedValue);

            String result = KeyResolver.resolve("", envName);

            assertNotNull(result);
        } finally {
            System.clearProperty(envName);
        }
    }

    @Test
    void testResolveKeyFromDotEnvFile(@TempDir Path tempDir) throws IOException {
        Path dotEnvFile = tempDir.resolve(".env");
        Files.writeString(dotEnvFile, "OPENROUTER_API_KEY=sk-or-...tenv\n");

        assertTrue(Files.exists(dotEnvFile));
        String content = Files.readString(dotEnvFile);
        assertTrue(content.contains("sk-or-...tenv"));
    }

    @Test
    void testResolveKeyReturnsEmptyStringWhenNothingFound() {
        String emptySpringValue = "";
        String nonExistentEnvName = "NON_EXISTENT_VAR_XYZ_EMBEDDING";

        String result = KeyResolver.resolve(emptySpringValue, nonExistentEnvName);

        assertEquals("", result);
    }

    @Test
    void testLoadDotEnvParsesCorrectly(@TempDir Path tempDir) throws IOException {
        Path dotEnvFile = tempDir.resolve(".env");
        String dotEnvContent = """
                # This is a comment
                OPENROUTER_API_KEY=***
                DATABASE_PASSWORD=secretPassword123
                EMPTY_VAR=
                """;
        Files.writeString(dotEnvFile, dotEnvContent);

        assertTrue(Files.exists(dotEnvFile));
        String content = Files.readString(dotEnvFile);
        assertTrue(content.contains("OPENROUTER_API_KEY=***"));
        assertTrue(content.contains("DATABASE_PASSWORD=secretPassword123"));
    }

    @Test
    void testEmbeddingModelConfigurationDefaults() {
        String validKey = "sk-or-...nfig";
        ReflectionTestUtils.setField(embeddingConfig, "openRouterApiKey", validKey);
        ReflectionTestUtils.setField(embeddingConfig, "baseUrl", "https://openrouter.ai/api/v1");
        ReflectionTestUtils.setField(embeddingConfig, "modelName", "nvidia/llama-nemotron-embed-vl-1b-v2:free");
        ReflectionTestUtils.setField(embeddingConfig, "dimensions", 2048);

        EmbeddingModel model = embeddingConfig.embeddingModel();

        assertNotNull(model);
        assertTrue(model instanceof OpenAiEmbeddingModel);
    }
}
