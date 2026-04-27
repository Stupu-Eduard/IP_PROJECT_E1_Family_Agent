package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.service.OcrService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@ActiveProfiles("test")
class OcrApplicationTests {

    @Autowired
    private OcrService ocrService;

    @Test
    void shouldExtractTextFromPdf() {
        File file = new File("src/main/resources/sample.pdf");

        try {
            String result = ocrService.extractTextFromPdf(file);
            System.out.println("--- OCR START ---");
            System.out.println(result);
            System.out.println("--- OCR END ---");
            assertNotNull(result);
            assertFalse(result.isEmpty());
        } catch (UnsatisfiedLinkError e) {
            Assumptions.assumeTrue(false, "Tesseract native library not available: " + e.getMessage());
        }
    }
}