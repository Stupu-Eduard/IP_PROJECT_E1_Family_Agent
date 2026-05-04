package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class TextBasedPdfExtractorTest {

    private TextBasedPdfExtractor extractor;

    private TextBasedPdfExtractor createInstance() {
        return new TextBasedPdfExtractor();
    }

    @BeforeEach
    void setUp() {
        extractor = createInstance();
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
}
