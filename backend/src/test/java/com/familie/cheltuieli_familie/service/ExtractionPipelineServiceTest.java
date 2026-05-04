package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ExtractionPipelineServiceTest {

    private ExtractionPipelineService pipelineService;
    private TextBasedPdfExtractor textExtractor;
    private BankOcrService ocrProcessor;
    private BankStatementParser bankParser;
    private StorageService storageService;

    private ExtractionPipelineService createService(
            TextBasedPdfExtractor textExtractor,
            BankOcrService ocrProcessor,
            BankStatementParser bankParser,
            StorageService storageService) {
        return new ExtractionPipelineService(textExtractor, ocrProcessor, bankParser, storageService);
    }

    @BeforeEach
    void setUp() {
        textExtractor = Mockito.mock(TextBasedPdfExtractor.class);
        ocrProcessor = Mockito.mock(BankOcrService.class);
        bankParser = Mockito.mock(BankStatementParser.class);
        storageService = Mockito.mock(StorageService.class);

        pipelineService = createService(textExtractor, ocrProcessor, bankParser, storageService);
    }

    @Test
    void processDocument_ShouldProcessDigitalDocument_WhenTextBased() throws Exception {
        File file = new File("test.pdf");
        String bank = "revolut";

        when(textExtractor.isTextBased(file)).thenReturn(true);
        when(textExtractor.extractText(file)).thenReturn("01/01/2026 Cumparaturi Mega 150.50");
        when(bankParser.parseText("01/01/2026 Cumparaturi Mega 150.50"))
                .thenReturn(Collections.singletonList(new Transaction("2026-01-01", 150.5, "Cumparaturi Mega", "expense", "RON")));
        when(storageService.save(any(List.class))).thenReturn(new StorageResult());

        List<Transaction> result = pipelineService.processDocument(file, bank);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2026-01-01", result.get(0).getDate());
    }

    @Test
    void processDocument_ShouldProcessScannedDocument_WhenNotTextBased() throws Exception {
        File file = new File("test.pdf");
        String bank = "revolut";

        when(textExtractor.isTextBased(file)).thenReturn(false);
        when(ocrProcessor.extractText(file, bank)).thenReturn("01/01/2026 Cumparaturi Mega 150.50");
        when(bankParser.parseText("01/01/2026 Cumparaturi Mega 150.50"))
                .thenReturn(Collections.singletonList(new Transaction("2026-01-01", 150.5, "Cumparaturi Mega", "expense", "RON")));
        when(storageService.save(any(List.class))).thenReturn(new StorageResult());

        List<Transaction> result = pipelineService.processDocument(file, bank);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2026-01-01", result.get(0).getDate());
    }
}
