package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtractionPipelineServiceTest {

    @Test
    void processDocumentShouldExtractTextAndParseTransactions() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor,
                parser,
                bankOcrService,
                ocrService,
                llmParser
        );

        File fakePdf = File.createTempFile("fake-text-pdf-", ".pdf");

        when(textExtractor.isTextBased(fakePdf)).thenReturn(true);
        when(textExtractor.extractText(fakePdf)).thenReturn("""
                10/03/2025 Lidl 100.50
                11/03/2025 Netflix 59.99
                """);

        List<Transaction> transactions = service.processDocument(fakePdf);

        assertNotNull(transactions);
        assertEquals(2, transactions.size());

        assertEquals(LocalDate.of(2025, 3, 10), transactions.get(0).getDate());
        assertEquals("Lidl", transactions.get(0).getDescription());
        assertEquals(100.50, transactions.get(0).getAmount());
        assertEquals("RON", transactions.get(0).getCurrency());
        assertEquals("EXPENSE", transactions.get(0).getType());

        assertEquals(LocalDate.of(2025, 3, 11), transactions.get(1).getDate());
        assertEquals("Netflix", transactions.get(1).getDescription());
        assertEquals(59.99, transactions.get(1).getAmount());
        assertEquals("RON", transactions.get(1).getCurrency());
        assertEquals("EXPENSE", transactions.get(1).getType());

        verify(textExtractor, times(1)).extractText(fakePdf);

        fakePdf.delete();
    }

    @Test
    void processDocumentShouldFallbackToOcrWhenNotTextBased() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor,
                parser,
                bankOcrService,
                ocrService,
                llmParser
        );

        File fakePdf = File.createTempFile("fake-image-pdf-", ".pdf");

        when(textExtractor.isTextBased(fakePdf)).thenReturn(false);
        when(ocrService.extractTextFromPdf(fakePdf)).thenReturn("""
                12/03/2025 Carrefour 250.00
                """);

        List<Transaction> transactions = service.processDocument(fakePdf);

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals(LocalDate.of(2025, 3, 12), transactions.get(0).getDate());
        assertEquals("Carrefour", transactions.get(0).getDescription());
        assertEquals(250.00, transactions.get(0).getAmount());

        verify(textExtractor, never()).extractText(fakePdf);
        verify(ocrService, times(1)).extractTextFromPdf(fakePdf);

        fakePdf.delete();
    }

    @Test
    void processDocumentShouldThrowIOExceptionWhenBothMethodsFail() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor,
                parser,
                bankOcrService,
                ocrService,
                llmParser
        );

        File fakePdf = File.createTempFile("fake-failing-pdf-", ".pdf");

        when(textExtractor.isTextBased(fakePdf)).thenReturn(false);
        when(ocrService.extractTextFromPdf(fakePdf)).thenThrow(new RuntimeException("OCR failed"));

        IOException exception = assertThrows(IOException.class, () -> service.processDocument(fakePdf));
        assertEquals("Failed to extract text from PDF using both text and OCR methods", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("OCR failed", exception.getCause().getMessage());

        fakePdf.delete();
    }

    @Test
    void processDocumentWithBankShouldUseTextExtractionWhenTextBased() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor,
                parser,
                bankOcrService,
                ocrService,
                llmParser
        );

        File fakePdf = File.createTempFile("fake-text-pdf-", ".pdf");

        when(textExtractor.isTextBased(fakePdf)).thenReturn(true);
        when(textExtractor.extractText(fakePdf)).thenReturn("""
                13/03/2025 Kaufland 75.25
                """);

        List<Transaction> transactions = service.processDocument(fakePdf, "ING");

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals(LocalDate.of(2025, 3, 13), transactions.get(0).getDate());
        assertEquals("Kaufland", transactions.get(0).getDescription());
        assertEquals(75.25, transactions.get(0).getAmount());

        verify(textExtractor, times(1)).extractText(fakePdf);
        verify(bankOcrService, never()).extractText(any(), any());

        fakePdf.delete();
    }

    @Test
    void processDocumentWithBankShouldUseBankOcrWhenNotTextBased() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor,
                parser,
                bankOcrService,
                ocrService,
                llmParser
        );

        File fakePdf = File.createTempFile("fake-image-pdf-", ".pdf");

        when(textExtractor.isTextBased(fakePdf)).thenReturn(false);
        when(bankOcrService.extractText(fakePdf, "Revolut")).thenReturn("""
                14/03/2025 Uber 34.50
                """);

        List<Transaction> transactions = service.processDocument(fakePdf, "Revolut");

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals(LocalDate.of(2025, 3, 14), transactions.get(0).getDate());
        assertEquals("Uber", transactions.get(0).getDescription());
        assertEquals(34.50, transactions.get(0).getAmount());

        verify(bankOcrService, times(1)).extractText(fakePdf, "Revolut");
        verify(ocrService, never()).extractTextFromPdf(fakePdf);

        fakePdf.delete();
    }

    @Test
    void processDocumentWithBankShouldFallbackToGenericOcrWhenBankOcrFails() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor,
                parser,
                bankOcrService,
                ocrService,
                llmParser
        );

        File fakePdf = File.createTempFile("fake-image-pdf-", ".pdf");

        when(textExtractor.isTextBased(fakePdf)).thenReturn(false);
        when(bankOcrService.extractText(fakePdf, "BT")).thenThrow(new RuntimeException("Bank OCR failed"));
        when(ocrService.extractTextFromPdf(fakePdf)).thenReturn("""
                15/03/2025 Shell 120.00
                """);

        List<Transaction> transactions = service.processDocument(fakePdf, "BT");

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals(LocalDate.of(2025, 3, 15), transactions.get(0).getDate());
        assertEquals("Shell", transactions.get(0).getDescription());
        assertEquals(120.00, transactions.get(0).getAmount());

        verify(bankOcrService, times(1)).extractText(fakePdf, "BT");
        verify(ocrService, times(1)).extractTextFromPdf(fakePdf);

        fakePdf.delete();
    }

    @Test
    void processDocumentWithBankShouldThrowIOExceptionWhenAllMethodsFail() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor,
                parser,
                bankOcrService,
                ocrService,
                llmParser
        );

        File fakePdf = File.createTempFile("fake-failing-pdf-", ".pdf");

        when(textExtractor.isTextBased(fakePdf)).thenReturn(false);
        when(bankOcrService.extractText(fakePdf, "Raiffeisen")).thenThrow(new RuntimeException("Bank OCR failed"));
        when(ocrService.extractTextFromPdf(fakePdf)).thenThrow(new RuntimeException("Generic OCR failed"));

        IOException exception = assertThrows(IOException.class, () -> service.processDocument(fakePdf, "Raiffeisen"));
        assertEquals("Failed to extract text from PDF using all available methods", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Generic OCR failed", exception.getCause().getMessage());

        fakePdf.delete();
    }
}
