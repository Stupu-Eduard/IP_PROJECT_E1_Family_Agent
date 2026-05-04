package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OCRPreProcessorTest {

    private OCRPreProcessor preProcessor;

    private OCRPreProcessor createInstance() {
        return new OCRPreProcessor();
    }

    @BeforeEach
    void setUp() {
        preProcessor = createInstance();
    }

    @Test
    void processImage_ShouldThrowException_WhenFileDoesNotExist() {
        File nonExistentFile = new File("non_existent_test_file.png");

        assertThrows(IllegalArgumentException.class, () -> {
            preProcessor.processImage(nonExistentFile, "revolut");
        });
    }

    @Test
    void processImage_ShouldThrowException_WhenFileIsEmpty() throws IOException {
        Path tempFile = Files.createTempFile("test_empty", ".png");

        assertThrows(IllegalArgumentException.class, () -> {
            preProcessor.processImage(tempFile.toFile(), "revolut");
        });

        Files.deleteIfExists(tempFile);
    }
}
