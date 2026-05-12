package com.familie.cheltuieli_familie.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OCRPreProcessorTest {

    @TempDir
    Path tempDir;

    private OCRPreProcessor preProcessor;
    private File validImageFile;
    private File validPdfFile;

    @BeforeEach
    void setUp() throws IOException {
        preProcessor = new OCRPreProcessor();

        validImageFile = tempDir.resolve("test_image.png").toFile();
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", validImageFile);

        validPdfFile = tempDir.resolve("test_doc.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(validPdfFile);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"revolut", "bt", "ing", "unknown_bank"})
    void processImage_ShouldHandleVariousBanks(String bankName) throws IOException {
        BufferedImage result = preProcessor.processImage(validImageFile, bankName);
        assertNotNull(result);
    }

    @Test
    void processImageShouldThrowExceptionForInvalidFile() {
        File invalidFile = new File("does-not-exist.png");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            preProcessor.processImage(invalidFile, "default");
        });

        assertTrue(exception.getMessage().contains("Couldn't read the image"));
    }

    @Test
    void processPdfShouldReturnProcessedImages() throws Exception {
        List<BufferedImage> results = preProcessor.processPdf(validPdfFile, "revolut");

        assertNotNull(results);
        assertEquals(1, results.size(), "Should process exactly 1 page from our dummy PDF");
        assertNotNull(results.get(0), "The processed image should not be null");
    }
}