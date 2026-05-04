package com.familie.cheltuieli_familie.service;

import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class BankOcrServiceTest {

    private BankOcrService bankOcrService;
    private OCRPreProcessor preProcessor;
    private BankingDictionaryCorrector corrector;
    private List<File> tempFiles;

    private BankOcrService createService(OCRPreProcessor preProcessor, BankingDictionaryCorrector corrector) {
        return new BankOcrService(preProcessor, corrector);
    }

    private File createTestPdf() throws IOException {
        File pdfFile = File.createTempFile("test_ocr_", ".pdf");
        tempFiles.add(pdfFile);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            document.save(pdfFile);
        }
        return pdfFile;
    }

    @BeforeEach
    void setUp() {
        preProcessor = Mockito.mock(OCRPreProcessor.class);
        corrector = Mockito.mock(BankingDictionaryCorrector.class);
        bankOcrService = createService(preProcessor, corrector);
        tempFiles = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        for (File file : tempFiles) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    @Test
    void extractText_ShouldThrowException_WhenFileDoesNotExist() {
        File nonExistentFile = new File("non_existent.pdf");

        assertThrows(Exception.class, () -> {
            bankOcrService.extractText(nonExistentFile, "revolut");
        });
    }

    @Test
    void extractText_ShouldThrowException_WhenFileIsNull() {
        assertThrows(Exception.class, () -> {
            bankOcrService.extractText(null, "revolut");
        });
    }

    @Test
    void extractText_ShouldThrowException_WhenFileIsInvalid() throws IOException {
        Path invalidFile = Files.createTempFile("test_invalid", ".pdf");
        tempFiles.add(invalidFile.toFile());

        assertThrows(Exception.class, () -> {
            bankOcrService.extractText(invalidFile.toFile(), "revolut");
        });
    }

    @Test
    void extractText_ShouldProcessPdfAndReturnText() throws Exception {
        File validPdf = createTestPdf();

        BufferedImage dummyImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        when(preProcessor.processImage(any(File.class), anyString())).thenReturn(dummyImage);
        when(corrector.correctText("TEXT_BRUT_OCR")).thenReturn("Text Corectat Final");

        try (MockedConstruction<Tesseract> mockedTesseract = Mockito.mockConstruction(Tesseract.class,
                (mock, context) -> {
                    when(mock.doOCR(any(BufferedImage.class))).thenReturn("TEXT_BRUT_OCR");
                })) {

            String result = bankOcrService.extractText(validPdf, "revolut");

            assertNotNull(result);
            assertTrue(result.contains("Text Corectat Final"));
            assertEquals(1, mockedTesseract.constructed().size());
        }
    }
}
