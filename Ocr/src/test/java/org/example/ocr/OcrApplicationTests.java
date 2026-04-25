package org.example.ocr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class OcrApplicationTests {

    @Autowired
    private OcrService ocrService;

    @Test
    void shouldExtractTextFromPdf() {

        File file = new File("src/main/resources/sample.pdf");

        String result = ocrService.extractTextFromPdf(file);
        System.out.println("--- OCR START ---");
        System.out.println(result);
        System.out.println("--- OCR END ---");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}