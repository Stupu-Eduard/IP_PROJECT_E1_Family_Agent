package com.familie.cheltuieli_familie.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextBasedPdfExtractorTest {

    private TextBasedPdfExtractor extractor;
    private List<File> tempFiles;

    private TextBasedPdfExtractor createInstance() {
        return new TextBasedPdfExtractor();
    }

    private File createTestPdf(String content) throws IOException {
        File pdfFile = File.createTempFile("test_pdf_", ".pdf");
        tempFiles.add(pdfFile);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(10, 700);
                contentStream.showText(content);
                contentStream.endText();
            }
            document.save(pdfFile);
        }
        return pdfFile;
    }

    @BeforeEach
    void setUp() {
        extractor = createInstance();
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
            extractor.extractText(nonExistentFile);
        });
    }

    @Test
    void isTextBased_ShouldThrowException_WhenFileDoesNotExist() {
        File nonExistentFile = new File("non_existent.pdf");

        assertThrows(Exception.class, () -> {
            extractor.isTextBased(nonExistentFile);
        });
    }

    @Test
    void extractText_ShouldThrowException_WhenFileIsNotValidPdf() throws IOException {
        File invalidPdfFile = File.createTempFile("test_invalid", ".pdf");
        tempFiles.add(invalidPdfFile);

        assertThrows(Exception.class, () -> {
            extractor.extractText(invalidPdfFile);
        });
    }

    @Test
    void extractText_ShouldReturnText_WhenPdfIsValid() throws Exception {
        String expectedText = "Hello World Document Digital";
        File validPdf = createTestPdf(expectedText);

        String extractedText = extractor.extractText(validPdf);

        assertNotNull(extractedText);
        assertTrue(extractedText.contains(expectedText));
    }

    @Test
    void isTextBased_ShouldReturnFalse_WhenTextIsShorterThan100Chars() throws Exception {
        String shortText = "Test scurt.";
        File validPdf = createTestPdf(shortText);

        boolean isTextBased = extractor.isTextBased(validPdf);

        assertFalse(isTextBased);
    }

    @Test
    void isTextBased_ShouldReturnTrue_WhenTextIsLongerThan100Chars() throws Exception {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            longText.append("CuvantTest ");
        }
        File validPdf = createTestPdf(longText.toString());

        boolean isTextBased = extractor.isTextBased(validPdf);

        assertTrue(isTextBased);
    }
}