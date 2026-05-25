package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OCRPreProcessorTest {

    private final OCRPreProcessor processor = new OCRPreProcessor();

    @Test
    void processImage_shouldThrowForNullFile() {
        assertThrows(IllegalArgumentException.class, () -> processor.processImage(null, "revolut"));
    }

    @Test
    void processImage_shouldThrowForNonExistentFile() {
        File nonExistent = new File("/nonexistent/path/image.png");
        assertThrows(IllegalArgumentException.class, () -> processor.processImage(nonExistent, "revolut"));
    }

    @Test
    void processImage_shouldProcessValidImage() throws IOException {
        // Create a simple test image
        Path tempFile = Files.createTempFile("test_image", ".png");
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
        javax.imageio.ImageIO.write(testImage, "png", tempFile.toFile());

        try {
            BufferedImage result = processor.processImage(tempFile.toFile(), "revolut");
            assertNotNull(result);
            assertTrue(result.getWidth() > 0);
            assertTrue(result.getHeight() > 0);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"unknown", "bt", "ing", "revolut"})
    void processImage_shouldHandleVariousBanks(String bank) throws IOException {
        Path tempFile = Files.createTempFile("test_image", ".png");
        BufferedImage testImage = new BufferedImage(200, 200, BufferedImage.TYPE_3BYTE_BGR);
        javax.imageio.ImageIO.write(testImage, "png", tempFile.toFile());

        try {
            BufferedImage result = processor.processImage(tempFile.toFile(), bank);
            assertNotNull(result);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void processPdf_shouldThrowForNonExistentFile() {
        File nonExistent = new File("/nonexistent/path/document.pdf");
        assertThrows(IOException.class, () -> processor.processPdf(nonExistent, "revolut"));
    }
}