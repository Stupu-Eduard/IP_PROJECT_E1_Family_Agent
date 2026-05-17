package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.exception.AiServiceException;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
    void testInit() {
        // init() is already called by Spring context; here we verify the tesseract mock was configured via setUp
        // No exception means init logic works with ReflectionTestUtils fields set
        assertDoesNotThrow(() -> {
            OcrService service = new OcrService();
            ReflectionTestUtils.setField(service, "tessDataPath", "/usr/share/tesseract-ocr/4.00/tessdata");
            ReflectionTestUtils.setField(service, "ocrLanguage", "ron+eng");
            service.init();
        });
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
    void testExtractTextFromPdfWithDirectTextExtraction() throws Exception {
        File pdfFile = tempDir.resolve("digital.pdf").toFile();
        // Create a minimal valid PDF file
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfFile)) {
            fos.write("%PDF-1.4\n1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n4 0 obj << /Length 44 >> stream\nBT /F1 12 Tf 100 700 Td (This is a long direct text extraction from a digital PDF document with more than fifty characters.) Tj ET\nendstream endobj\nxref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000058 00000 n\n0000000115 00000 n\n0000000214 00000 n\ntrailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n310\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        // Use mocked construction for PDFTextStripper to return long text
        try (MockedConstruction<PDFTextStripper> mocked = mockConstruction(PDFTextStripper.class,
                (mock, context) -> when(mock.getText(any(PDDocument.class))).thenReturn(
                        "This is a long direct text extraction from a digital PDF document with more than fifty characters."))) {
            String result = ocrService.extractTextFromPdf(pdfFile);
            assertNotNull(result);
            assertTrue(result.length() > 50);
        }
    }

    @Test
    void testExtractTextFromPdfWithOcrFallback() throws Exception {
        File pdfFile = tempDir.resolve("scanned.pdf").toFile();
        // Create a minimal valid PDF file
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfFile)) {
            fos.write("%PDF-1.4\n1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n4 0 obj << /Length 44 >> stream\nBT /F1 12 Tf 100 700 Td (Short) Tj ET\nendstream endobj\nxref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000058 00000 n\n0000000115 00000 n\n0000000214 00000 n\ntrailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n310\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try (MockedConstruction<PDFTextStripper> mockedStripper = mockConstruction(PDFTextStripper.class,
                (mock, context) -> when(mock.getText(any(PDDocument.class))).thenReturn("Short"));
             MockedConstruction<PDFRenderer> mockedRenderer = mockConstruction(PDFRenderer.class,
                     (mock, context) -> when(mock.renderImageWithDPI(anyInt(), eq(300f))).thenReturn(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)));
             MockedConstruction<org.apache.pdfbox.Loader> ignored = mockConstruction(org.apache.pdfbox.Loader.class)) {

            when(tesseract.doOCR(any(BufferedImage.class))).thenReturn("OCR extracted text from scanned PDF page");

            String result = ocrService.extractTextFromPdf(pdfFile);
            assertNotNull(result);
            assertTrue(result.contains("OCR extracted text"));
        }
    }

    @Test
    void testExtractTextFromPdfWithDirectTextExtractionFailure() throws Exception {
        File pdfFile = tempDir.resolve("broken.pdf").toFile();
        // Create a minimal valid PDF file
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfFile)) {
            fos.write("%PDF-1.4\n1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n4 0 obj << /Length 44 >> stream\nBT /F1 12 Tf 100 700 Td (Text) Tj ET\nendstream endobj\nxref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000058 00000 n\n0000000115 00000 n\n0000000214 00000 n\ntrailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n310\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try (MockedConstruction<PDFTextStripper> mockedStripper = mockConstruction(PDFTextStripper.class,
                (mock, context) -> when(mock.getText(any(PDDocument.class))).thenThrow(new IOException("Stripper failed")));
             MockedConstruction<PDFRenderer> mockedRenderer = mockConstruction(PDFRenderer.class,
                     (mock, context) -> when(mock.renderImageWithDPI(anyInt(), eq(300f))).thenReturn(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)))) {

            when(tesseract.doOCR(any(BufferedImage.class))).thenReturn("OCR fallback text after direct extraction failure");

            String result = ocrService.extractTextFromPdf(pdfFile);
            assertNotNull(result);
            assertTrue(result.contains("OCR fallback text"));
        }
    }

    @Test
    void testExtractTextDirectlyIOException() throws Exception {
        File pdfFile = tempDir.resolve("direct_fail.pdf").toFile();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfFile)) {
            fos.write("%PDF-1.4\n1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n4 0 obj << /Length 44 >> stream\nBT /F1 12 Tf 100 700 Td (Text) Tj ET\nendstream endobj\nxref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000058 00000 n\n0000000115 00000 n\n0000000214 00000 n\ntrailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n310\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try (MockedConstruction<PDFTextStripper> mockedStripper = mockConstruction(PDFTextStripper.class,
                (mock, context) -> when(mock.getText(any(PDDocument.class))).thenThrow(new IOException("Direct extraction IO error")));
             MockedConstruction<PDFRenderer> mockedRenderer = mockConstruction(PDFRenderer.class,
                     (mock, context) -> when(mock.renderImageWithDPI(anyInt(), eq(300f))).thenReturn(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)))) {

            when(tesseract.doOCR(any(BufferedImage.class))).thenReturn("Recovered via OCR");

            String result = ocrService.extractTextFromPdf(pdfFile);
            assertEquals("Recovered via OCR\n", result);
        }
    }

    @Test
    void testExtractTextWithOcrIOException() throws Exception {
        File pdfFile = tempDir.resolve("ocr_io_fail.pdf").toFile();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfFile)) {
            fos.write("%PDF-1.4\n1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n4 0 obj << /Length 44 >> stream\nBT /F1 12 Tf 100 700 Td (Text) Tj ET\nendstream endobj\nxref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000058 00000 n\n0000000115 00000 n\n0000000214 00000 n\ntrailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n310\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try (MockedConstruction<PDFTextStripper> mockedStripper = mockConstruction(PDFTextStripper.class,
                (mock, context) -> when(mock.getText(any(PDDocument.class))).thenReturn("Short"));
             MockedConstruction<PDFRenderer> mockedRenderer = mockConstruction(PDFRenderer.class,
                     (mock, context) -> when(mock.renderImageWithDPI(anyInt(), eq(300f))).thenThrow(new IOException("Renderer IO error")))) {

            AiServiceException ex = assertThrows(AiServiceException.class, () -> ocrService.extractTextFromPdf(pdfFile));
            assertTrue(ex.getMessage().contains("Failed to process PDF for OCR"));
        }
    }

    @Test
    void testExtractTextWithOcrTesseractException() throws Exception {
        File pdfFile = tempDir.resolve("ocr_tess_fail.pdf").toFile();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfFile)) {
            fos.write("%PDF-1.4\n1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n4 0 obj << /Length 44 >> stream\nBT /F1 12 Tf 100 700 Td (Text) Tj ET\nendstream endobj\nxref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000058 00000 n\n0000000115 00000 n\n0000000214 00000 n\ntrailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n310\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try (MockedConstruction<PDFTextStripper> mockedStripper = mockConstruction(PDFTextStripper.class,
                (mock, context) -> when(mock.getText(any(PDDocument.class))).thenReturn("Short"));
             MockedConstruction<PDFRenderer> mockedRenderer = mockConstruction(PDFRenderer.class,
                     (mock, context) -> when(mock.renderImageWithDPI(anyInt(), eq(300f))).thenReturn(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)))) {

            when(tesseract.doOCR(any(BufferedImage.class))).thenThrow(new TesseractException("Tesseract failed"));

            AiServiceException ex = assertThrows(AiServiceException.class, () -> ocrService.extractTextFromPdf(pdfFile));
            assertTrue(ex.getMessage().contains("Failed to process PDF for OCR"));
        }
    }

    @Test
    void testExtractTextFromPdfIoException() {
        File nonExistent = new File(tempDir.toFile(), "missing.pdf");

        AiServiceException ex = assertThrows(AiServiceException.class,
                () -> ocrService.extractTextFromPdf(nonExistent));
        assertTrue(ex.getMessage().contains("Failed to process PDF"));
    }
}
