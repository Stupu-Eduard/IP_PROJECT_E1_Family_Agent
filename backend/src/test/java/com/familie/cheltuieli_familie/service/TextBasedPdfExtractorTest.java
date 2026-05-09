package com.familie.cheltuieli_familie.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class TextBasedPdfExtractorTest {

    private final TextBasedPdfExtractor extractor = new TextBasedPdfExtractor();

    @Test
    void extractTextShouldReadTextFromDigitalPdf() throws Exception {
        File pdfFile = createTempPdf("""
                10/03/2025 Lidl 100.50
                11/03/2025 Netflix 59.99
                """);

        String text = extractor.extractText(pdfFile);

        assertNotNull(text);
        assertTrue(text.contains("Lidl"));
        assertTrue(text.contains("Netflix"));
        assertTrue(text.contains("100.50"));

        pdfFile.delete();
    }

    @Test
    void isTextBasedShouldReturnTrueForPdfWithEnoughText() throws Exception {
        File pdfFile = createTempPdf("""
                Extras de cont bancar pentru test OCR.
                10/03/2025 Lidl 100.50
                11/03/2025 Netflix 59.99
                12/03/2025 Kaufland 45.99
                Acest text este suficient de lung pentru ca metoda isTextBased sa returneze true.
                """);

        boolean result = extractor.isTextBased(pdfFile);

        assertTrue(result);

        pdfFile.delete();
    }

    private File createTempPdf(String content) throws Exception {
        File file = File.createTempFile("text-based-test-", ".pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(50, 700);

                for (String line : content.split("\\R")) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -20);
                }

                contentStream.endText();
            }

            document.save(file);
        }

        return file;
    }
}