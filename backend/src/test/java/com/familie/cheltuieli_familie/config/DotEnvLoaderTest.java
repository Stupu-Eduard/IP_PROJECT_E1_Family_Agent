package com.familie.cheltuieli_familie.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DotEnvLoaderTest {

    private Path envFile;

    @BeforeEach
    void setUp() {
        envFile = Path.of(".env").toAbsolutePath().normalize();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(envFile);
        // Also clean up if a directory was created
        if (Files.exists(envFile) && Files.isDirectory(envFile)) {
            Files.deleteIfExists(envFile);
        }
    }

    @Test
    void loadReturnsEmptyMapWhenNoEnvFileExists() {
        Map<String, String> result = DotEnvLoader.load();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadParsesEnvFileCorrectly() throws IOException {
        String content = """
                KEY1=value1
                KEY2=value2
                """;
        Files.writeString(envFile, content);

        Map<String, String> result = DotEnvLoader.load();

        assertEquals("value1", result.get("KEY1"));
        assertEquals("value2", result.get("KEY2"));
    }

    @Test
    void loadSkipsCommentsAndEmptyLines() throws IOException {
        String content = """
                # This is a comment
                KEY1=value1

                # Another comment
                KEY2=value2
                """;
        Files.writeString(envFile, content);

        Map<String, String> result = DotEnvLoader.load();

        assertEquals(2, result.size());
        assertEquals("value1", result.get("KEY1"));
        assertEquals("value2", result.get("KEY2"));
    }

    @Test
    void loadHandlesLinesWithoutEquals() throws IOException {
        String content = """
                KEY1=value1
                INVALID_LINE_NO_EQUALS
                KEY2=value2
                """;
        Files.writeString(envFile, content);

        Map<String, String> result = DotEnvLoader.load();

        assertEquals(2, result.size());
        assertEquals("value1", result.get("KEY1"));
        assertEquals("value2", result.get("KEY2"));
    }

    @Test
    void loadHandlesLineStartingWithEquals() throws IOException {
        String content = """
                =value1
                KEY2=value2
                """;
        Files.writeString(envFile, content);

        Map<String, String> result = DotEnvLoader.load();

        assertEquals(1, result.size());
        assertEquals("value2", result.get("KEY2"));
    }

    @Test
    void loadHandlesMultipleEqualsSigns() throws IOException {
        String content = """
                KEY1=value=with=equals
                """;
        Files.writeString(envFile, content);

        Map<String, String> result = DotEnvLoader.load();

        assertEquals("value=with=equals", result.get("KEY1"));
    }

    @Test
    void loadReturnsEmptyMapWhenPathIsDirectory() throws IOException {
        // Create a directory named .env to trigger IOException in tryLoad
        Files.createDirectories(envFile);

        Map<String, String> result = DotEnvLoader.load();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        Files.deleteIfExists(envFile);
    }
}
