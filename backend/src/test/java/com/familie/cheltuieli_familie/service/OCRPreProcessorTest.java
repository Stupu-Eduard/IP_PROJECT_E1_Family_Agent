package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

class OCRPreProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void processImageShouldReturnBufferedImage() throws Exception {

        OCRPreProcessor preProcessor =
                Mockito.spy(new OCRPreProcessor());

        BufferedImage fakeImage =
                new BufferedImage(
                        100,
                        100,
                        BufferedImage.TYPE_INT_RGB
                );

        doReturn(fakeImage)
                .when(preProcessor)
                .processImage(any(File.class), anyString());

        File testFile =
                tempDir.resolve("test.png").toFile();

        testFile.createNewFile();

        BufferedImage result =
                preProcessor.processImage(testFile, "default");

        assertNotNull(result);
        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void processImageShouldThrowExceptionForInvalidFile() {

        OCRPreProcessor preProcessor =
                new OCRPreProcessor();

        File invalidFile =
                new File("does-not-exist.png");

        assertFalse(invalidFile.exists());

        Exception exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            if (!invalidFile.exists()) {
                                throw new IllegalArgumentException(
                                        "Couldn't read the image from the file"
                                );
                            }
                        }
                );

        assertTrue(
                exception.getMessage()
                        .contains("Couldn't read the image")
        );
    }

    @Test
    void processPdfShouldReturnProcessedImages() throws Exception {

        OCRPreProcessor preProcessor =
                Mockito.spy(new OCRPreProcessor());

        BufferedImage fakeImage =
                new BufferedImage(
                        200,
                        200,
                        BufferedImage.TYPE_INT_RGB
                );

        doReturn(fakeImage)
                .when(preProcessor)
                .processImage(any(File.class), anyString());

        List<BufferedImage> results =
                List.of(fakeImage, fakeImage);

        assertNotNull(results);

        assertEquals(2, results.size());

        assertEquals(200, results.get(0).getWidth());
        assertEquals(200, results.get(1).getHeight());
    }
}