package org.example.ocrm2;

import org.example.ocrm2.service.OcrService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") 
class MainTests {

    @Autowired
    private OcrService ocrService;

    @Test
    void shouldExtractTextFromPdf() {

        File file = new File("src/main/resources/sample.pdf");

        String result = ocrService.extractTextFromPdf(file);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
