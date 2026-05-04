package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BankOcrServiceTest {

    private BankOcrService bankOcrService;
    private OCRPreProcessor preProcessor;
    private BankingDictionaryCorrector corrector;

    private BankOcrService createService(OCRPreProcessor preProcessor, BankingDictionaryCorrector corrector) {
        return new BankOcrService(preProcessor, corrector);
    }

    @BeforeEach
    void setUp() {
        preProcessor = Mockito.mock(OCRPreProcessor.class);
        corrector = Mockito.mock(BankingDictionaryCorrector.class);
        bankOcrService = createService(preProcessor, corrector);
    }

    @Test
    void extractText_ShouldThrowException_WhenFileDoesNotExist() {
        File nonExistentFile = new File("non_existent.pdf");

        assertThrows(Exception.class, () -> {
            bankOcrService.extractText(nonExistentFile, "revolut");
        });
    }
}
