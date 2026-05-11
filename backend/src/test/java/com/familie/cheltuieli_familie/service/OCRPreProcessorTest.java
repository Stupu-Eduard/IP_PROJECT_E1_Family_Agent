package com.familie.cheltuieli_familie.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OCRPreProcessorTest {

    private final OCRPreProcessor preProcessor = new OCRPreProcessor();

    @ParameterizedTest
    @ValueSource(strings = {"default", "revolut", "bt", "ing"})
    void processImageShouldReturnProcessedImageForValidImage(String bank) throws Exception {
        File imageFile = createTempImage();

        BufferedImage result = preProcessor.processImage(imageFile, bank);

        assertNotNull(result);
        assertTrue(result.getWidth() > 0);
        assertTrue(result.getHeight() > 0);

        assertTrue(imageFile.delete());
    }

    @Test
    void processImageShouldThrowExceptionForInvalidImage() {
        File invalidFile = new File("file-care-nu-exista.png");

        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> preProcessor.processImage(invalidFile, "default")
        );

        assertTrue(exception.getMessage().contains("Couldn't read the image"));
    }

    @Test
    void processPdfShouldReturnProcessedImagesForEachPage() throws Exception {
        File pdfFile = createTempPdfWithTwoPages();

        List<BufferedImage> results = preProcessor.processPdf(pdfFile, "bt");

        assertNotNull(results);
        assertEquals(2, results.size());

        assertNotNull(results.get(0));
        assertNotNull(results.get(1));

        assertTrue(results.get(0).getWidth() > 0);
        assertTrue(results.get(0).getHeight() > 0);
        assertTrue(results.get(1).getWidth() > 0);
        assertTrue(results.get(1).getHeight() > 0);

        assertTrue(pdfFile.delete());
    }

    private File createTempImage() throws Exception {
        File file = File.createTempFile("ocr-preprocessor-test-", ".png");

        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 300, 300);

        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Arial", Font.PLAIN, 20));
        graphics.drawString("10/03/2025 Lidl 100.50 RON", 20, 150);

        graphics.dispose();

        ImageIO.write(image, "png", file);
        return file;
    }

    private File createTempPdfWithTwoPages() throws Exception {
        File pdfFile = File.createTempFile("ocr-preprocessor-test-", ".pdf");

        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.addPage(new PDPage());
            document.save(pdfFile);
        }

        return pdfFile;
    }
}
