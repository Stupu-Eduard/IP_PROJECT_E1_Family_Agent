package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.exception.AiServiceException;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrServiceTest {

    @InjectMocks
    private OcrService ocrService;

    @Mock
    private ITesseract tesseract;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ocrService, "tessDataPath", "/usr/share/tesseract-ocr/4.00/tessdata");
        ReflectionTestUtils.setField(ocrService, "ocrLanguage", "ron+eng");
        ReflectionTestUtils.setField(ocrService, "tesseract", tesseract);
    }

    @Test
    void testExtractTextFromImageSuccess() throws TesseractException {
        File imageFile = tempDir.resolve("test.png").toFile();
        when(tesseract.doOCR(imageFile)).thenReturn("Extracted text");

        String result = ocrService.extractTextFromImage(imageFile);

        assertEquals("Extracted text", result);
        verify(tesseract).doOCR(imageFile);
    }

    @Test
    void testExtractTextFromImageThrowsAiServiceException() throws TesseractException {
        File imageFile = tempDir.resolve("test.png").toFile();
        when(tesseract.doOCR(imageFile)).thenThrow(new TesseractException("OCR error"));

        AiServiceException ex = assertThrows(AiServiceException.class,
                () -> ocrService.extractTextFromImage(imageFile));
        assertTrue(ex.getMessage().contains("Failed to process image for OCR"));
    }

    @Test
    void testExtractTextFromPdfIoException() {
        File nonExistent = new File(tempDir.toFile(), "missing.pdf");

        AiServiceException ex = assertThrows(AiServiceException.class,
                () -> ocrService.extractTextFromPdf(nonExistent));
        assertTrue(ex.getMessage().contains("Failed to process PDF"));
    }
}
