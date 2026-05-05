package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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

    private Path createTempImageFile() throws IOException {
        Path tempFile = Files.createTempFile("test_image", ".png");
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(img, "png", tempFile.toFile());
        return tempFile;
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

    @Test
    void processImage_ShouldProcessSuccessfully_WhenBankIsRevolut() throws IOException {
        Path tempFilePath = createTempImageFile();

        BufferedImage result = preProcessor.processImage(tempFilePath.toFile(), "revolut");
        assertNotNull(result);

        Files.deleteIfExists(tempFilePath);
    }

    @Test
    void processImage_ShouldProcessSuccessfully_WhenBankIsBt() throws IOException {
        Path tempFilePath = createTempImageFile();

        BufferedImage result = preProcessor.processImage(tempFilePath.toFile(), "bt");
        assertNotNull(result);

        Files.deleteIfExists(tempFilePath);
    }

    @Test
    void processImage_ShouldProcessSuccessfully_WhenBankIsIng() throws IOException {
        Path tempFilePath = createTempImageFile();

        BufferedImage result = preProcessor.processImage(tempFilePath.toFile(), "ing");
        assertNotNull(result);

        Files.deleteIfExists(tempFilePath);
    }

    @Test
    void processImage_ShouldProcessSuccessfully_WhenBankIsUnknown() throws IOException {
        Path tempFilePath = createTempImageFile();

        BufferedImage result = preProcessor.processImage(tempFilePath.toFile(), "unknown");
        assertNotNull(result);

        Files.deleteIfExists(tempFilePath);
    }
}
