package com.familie.cheltuieli_familie.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class KeyResolverTest {

    private Path envFile;

    @BeforeEach
    void setUp() {
        envFile = Path.of(".env").toAbsolutePath().normalize();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(envFile);
    }

    @Test
    void resolveReturnsSpringValueWhenNotEmpty() {
        String result = KeyResolver.resolve("spring-key", "ANY_ENV");
        assertEquals("spring-key", result);
    }

    @Test
    void resolveReturnsEmptyWhenSpringValueIsNullAndEnvNotSet() {
        // Covers the missing branch: springValue == null
        String result = KeyResolver.resolve(null, "NON_EXISTENT_ENV_NULL_TEST");
        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    void resolveReturnsEmptyWhenSpringValueIsEmptyAndEnvNotSet() {
        String result = KeyResolver.resolve("", "NON_EXISTENT_ENV_EMPTY_TEST");
        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    void resolveReturnsEnvValueWhenSpringValueIsEmptyAndEnvExists() {
        // Uses a known environment variable (PATH) to cover the env != null branch
        String result = KeyResolver.resolve("", "PATH");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void resolveReturnsDotEnvValueWhenSpringAndEnvAreEmpty() throws IOException {
        Files.writeString(envFile, "DOTENV_TEST_KEY=dotenv-value\n");
        String result = KeyResolver.resolve("", "DOTENV_TEST_KEY");
        assertEquals("dotenv-value", result);
    }

    @Test
    void resolveReturnsEmptyStringWhenNothingFound() {
        String result = KeyResolver.resolve("", "NON_EXISTENT_VAR_12345");
        assertEquals("", result);
    }
}
