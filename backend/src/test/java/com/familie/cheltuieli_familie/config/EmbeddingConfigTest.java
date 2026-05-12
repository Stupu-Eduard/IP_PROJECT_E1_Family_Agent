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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingConfigTest {

    private EmbeddingConfig embeddingConfig;
    private Map<String, String> mockEnv = new HashMap<>();

    @BeforeEach
    void setUp() {
        mockEnv.clear();
        embeddingConfig = new EmbeddingConfig() {
            @Override
            protected String getEnv(String name) {
                return mockEnv.get(name);
            }
        };
    }

    @Test
    void testEmbeddingModelBeanCreationWithValidKey() {
        // Arrange
        String validKey = "sk-or-v1-test-valid-key-12345";
        ReflectionTestUtils.setField(embeddingConfig, "openRouterApiKey", validKey);

        // Act
        EmbeddingModel embeddingModel = embeddingConfig.embeddingModel();

        // Assert
        assertNotNull(embeddingModel);
        assertInstanceOf(OpenAiEmbeddingModel.class, embeddingModel);
    }

    @Test
    void testEmbeddingModelBeanCreationThrowsExceptionWhenKeyIsEmpty() {
        // Arrange
        ReflectionTestUtils.setField(embeddingConfig, "openRouterApiKey", "");
        // Ensure no fallback keys exist in mock env or real files
        embeddingConfig = new EmbeddingConfig() {
            @Override
            protected String getEnv(String name) { return null; }
            @Override
            Map<String, String> loadDotEnv() { return Map.of(); }
        };
        ReflectionTestUtils.setField(embeddingConfig, "openRouterApiKey", "");

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> embeddingConfig.embeddingModel()
        );
        assertEquals("OPENROUTER_API_KEY is required for embeddings.", exception.getMessage());
    }

    @Test
    void testEmbeddingModelBeanCreationThrowsExceptionWhenKeyIsNull() {
        // Arrange
        embeddingConfig = new EmbeddingConfig() {
            @Override
            protected String getEnv(String name) { return null; }
            @Override
            Map<String, String> loadDotEnv() { return Map.of(); }
        };
        ReflectionTestUtils.setField(embeddingConfig, "openRouterApiKey", null);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> embeddingConfig.embeddingModel()
        );
        assertEquals("OPENROUTER_API_KEY is required for embeddings.", exception.getMessage());
    }

    @Test
    void testResolveKeyFromSpringValue() {
        // Arrange
        String springValue = "sk-or-v1-spring-value";
        String envName = "OPENROUTER_API_KEY";

        // Act
        String result = ReflectionTestUtils.invokeMethod(
                embeddingConfig,
                "resolveKey",
                springValue,
                envName
        );

        // Assert
        assertEquals(springValue, result);
    }

    @Test
    void testResolveKeyFromSystemEnvironment() {
        // Arrange
        String envName = "TEST_ENV_VAR_EMBEDDING";
        String expectedValue = "sk-or-v1-env-var";
        mockEnv.put(envName, expectedValue);

        // Act
        String result = ReflectionTestUtils.invokeMethod(
                embeddingConfig,
                "resolveKey",
                "",  // empty spring value
                envName
        );

        // Assert
        assertEquals(expectedValue, result);
    }

    @Test
    void testResolveKeyFromDotEnvFile(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path dotEnvFile = tempDir.resolve(".env");
        Files.writeString(dotEnvFile, "OPENROUTER_API_KEY=sk-or-v1-from-dotenv\n");

        // Save current working directory
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        try {
            // Use real EmbeddingConfig but override getEnv to avoid picking up system environment
            EmbeddingConfig config = new EmbeddingConfig() {
                @Override
                protected String getEnv(String name) {
                    return null;
                }
            };
            
            // Act
            String result = ReflectionTestUtils.invokeMethod(
                    config,
                    "resolveKey",
                    "",  // empty spring value
                    "OPENROUTER_API_KEY"
            );

            // Assert
            assertEquals("sk-or-v1-from-dotenv", result);
        } finally {
            // Restore original working directory
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void testResolveKeyReturnsEmptyStringWhenNothingFound() {
        // Arrange
        String emptySpringValue = "";
        String nonExistentEnvName = "NON_EXISTENT_VAR_XYZ_EMBEDDING";
        
        embeddingConfig = new EmbeddingConfig() {
            @Override
            protected String getEnv(String name) { return null; }
            @Override
            Map<String, String> loadDotEnv() { return Map.of(); }
        };

        // Act
        String result = ReflectionTestUtils.invokeMethod(
                embeddingConfig,
                "resolveKey",
                emptySpringValue,
                nonExistentEnvName
        );

        // Assert
        assertEquals("", result);
    }

    @Test
    void testLoadDotEnvParsesCorrectly(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path dotEnvFile = tempDir.resolve(".env");
        String dotEnvContent = """
                # This is a comment
                OPENROUTER_API_KEY=sk-or-v1-test-key
                DATABASE_PASSWORD=secretPassword123
                EMPTY_VAR=
                """;
        Files.writeString(dotEnvFile, dotEnvContent);

        // Save and change working directory
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        try {
            // Act
            EmbeddingConfig config = new EmbeddingConfig();
            var envMap = ReflectionTestUtils.invokeMethod(config, "loadDotEnv");

            // Assert
            assertNotNull(envMap);
            assertTrue(envMap instanceof Map);
            Map<String, String> map = (Map<String, String>) envMap;
            assertEquals("sk-or-v1-test-key", map.get("OPENROUTER_API_KEY"));
            assertEquals("secretPassword123", map.get("DATABASE_PASSWORD"));
            assertEquals("", map.get("EMPTY_VAR"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void testEmbeddingModelConfigurationDefaults() {
        // Arrange
        String validKey = "sk-or-v1-test-key-for-config";
        ReflectionTestUtils.setField(embeddingConfig, "openRouterApiKey", validKey);

        // Act
        EmbeddingModel model = embeddingConfig.embeddingModel();

        // Assert
        assertNotNull(model);
        assertTrue(model instanceof OpenAiEmbeddingModel);
    }
}
