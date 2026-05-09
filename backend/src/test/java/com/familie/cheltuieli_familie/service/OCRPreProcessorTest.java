package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class OCRPreProcessorTest {

    private final OCRPreProcessor preProcessor = new OCRPreProcessor();

    @Test
    void processImageShouldReturnProcessedImageForValidImage() throws Exception {
        File imageFile = createTempImage();

        BufferedImage result = preProcessor.processImage(imageFile, "default");

        assertNotNull(result);
        assertTrue(result.getWidth() > 0);
        assertTrue(result.getHeight() > 0);

        imageFile.delete();
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

    private File createTempImage() throws Exception {
        File file = File.createTempFile("ocr-preprocessor-test-", ".png");

        BufferedImage image = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 300, 100);

        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Arial", Font.PLAIN, 20));
        graphics.drawString("10/03/2025 Lidl 100.50", 20, 50);

        graphics.dispose();

        ImageIO.write(image, "png", file);
        return file;
    }
}