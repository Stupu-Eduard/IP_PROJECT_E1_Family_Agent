package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    void processImage_shouldHandleDefaultBank() throws IOException {
        Path tempFile = Files.createTempFile("test_image", ".png");
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
        javax.imageio.ImageIO.write(testImage, "png", tempFile.toFile());

        try {
            BufferedImage result = processor.processImage(tempFile.toFile(), "unknown");
            assertNotNull(result);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void processImageShouldThrowExceptionForInvalidFile() {
        File invalidFile = new File("does-not-exist.png");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            preProcessor.processImage(invalidFile, "default");
        });

        assertTrue(exception.getMessage().contains("nu a fost găsit"));
    }

    @Test
    void processImage_shouldHandleBTBank() throws IOException {
        Path tempFile = Files.createTempFile("test_image", ".png");
        BufferedImage testImage = new BufferedImage(200, 200, BufferedImage.TYPE_3BYTE_BGR);
        javax.imageio.ImageIO.write(testImage, "png", tempFile.toFile());
    void processImage_ShouldThrowExceptionForNullFile() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            preProcessor.processImage(null, "revolut");
        });

        assertTrue(exception.getMessage().contains("nu a fost găsit"));
    }

    @Test
    void processPdfShouldReturnProcessedImages() throws Exception {
        List<BufferedImage> results = preProcessor.processPdf(validPdfFile, "revolut");

        try {
            BufferedImage result = processor.processImage(tempFile.toFile(), "bt");
            assertNotNull(result);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void processImage_shouldHandleINGBank() throws IOException {
        Path tempFile = Files.createTempFile("test_image", ".png");
        BufferedImage testImage = new BufferedImage(200, 200, BufferedImage.TYPE_3BYTE_BGR);
        javax.imageio.ImageIO.write(testImage, "png", tempFile.toFile());

        try {
            BufferedImage result = processor.processImage(tempFile.toFile(), "ing");
            assertNotNull(result);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void processImage_shouldHandleRevolutBank() throws IOException {
        Path tempFile = Files.createTempFile("test_image", ".png");
        BufferedImage testImage = new BufferedImage(200, 200, BufferedImage.TYPE_3BYTE_BGR);
        javax.imageio.ImageIO.write(testImage, "png", tempFile.toFile());

        try {
            BufferedImage result = processor.processImage(tempFile.toFile(), "revolut");
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
