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

    @Test
    void processDocument_textBasedGoodCoverageShortText_returnsRegexResults() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor, parser, bankOcrService, ocrService, llmParser);

        File fakePdf = File.createTempFile("fake-text-good-", ".pdf");
        when(textExtractor.isTextBased(fakePdf)).thenReturn(true);
        when(textExtractor.extractText(fakePdf)).thenReturn("13/03/2025 Uber 34.50");

        List<Transaction> transactions = service.processDocument(fakePdf);

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals("Uber", transactions.get(0).getDescription());
        verify(llmParser, never()).parse(any());

        fakePdf.delete();
    }

    @Test
    void processDocument_textBasedLowCoverage_triggersLlmFallback() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor, parser, bankOcrService, ocrService, llmParser);

        File fakePdf = File.createTempFile("fake-text-low-", ".pdf");
        String longText = "13/03/2025 Uber 34.50\n" + "Header line with lots of padding to make text longer than five hundred characters. ".repeat(10);
        when(textExtractor.isTextBased(fakePdf)).thenReturn(true);
        when(textExtractor.extractText(fakePdf)).thenReturn(longText);

        BankStatementLlmParser.ParsedTransaction pt = new BankStatementLlmParser.ParsedTransaction();
        pt.setDate("14/03/2025");
        pt.setDescription("LLM Desc");
        pt.setAmount(99.99);
        pt.setCurrency("RON");
        pt.setType("EXPENSE");
        when(llmParser.parse(longText)).thenReturn(List.of(pt));

        List<Transaction> transactions = service.processDocument(fakePdf);

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals("LLM Desc", transactions.get(0).getDescription());
        assertEquals(99.99, transactions.get(0).getAmount());
        verify(llmParser, times(1)).parse(longText);

        fakePdf.delete();
    }

    @Test
    void processDocument_ocrPathGoodCoverage_returnsRegexResults() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor, parser, bankOcrService, ocrService, llmParser);

        File fakePdf = File.createTempFile("fake-ocr-good-", ".pdf");
        when(textExtractor.isTextBased(fakePdf)).thenReturn(false);
        when(ocrService.extractTextFromPdf(fakePdf)).thenReturn("15/03/2025 Shell 120.00");

        List<Transaction> transactions = service.processDocument(fakePdf);

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals("Shell", transactions.get(0).getDescription());
        verify(llmParser, never()).parse(any());

        fakePdf.delete();
    }

    @Test
    void processDocument_ocrPathLowCoverage_triggersLlmFallback() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor, parser, bankOcrService, ocrService, llmParser);

        File fakePdf = File.createTempFile("fake-ocr-low-", ".pdf");
        String longText = "15/03/2025 Shell 120.00\n" + "More padding to exceed five hundred characters threshold easily. ".repeat(10);
        when(textExtractor.isTextBased(fakePdf)).thenReturn(false);
        when(ocrService.extractTextFromPdf(fakePdf)).thenReturn(longText);

        BankStatementLlmParser.ParsedTransaction pt = new BankStatementLlmParser.ParsedTransaction();
        pt.setDate("16/03/2025");
        pt.setDescription("OCR LLM Desc");
        pt.setAmount(200.00);
        pt.setCurrency("EUR");
        pt.setType("INCOME");
        when(llmParser.parse(longText)).thenReturn(List.of(pt));

        List<Transaction> transactions = service.processDocument(fakePdf);

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals("OCR LLM Desc", transactions.get(0).getDescription());
        assertEquals(200.00, transactions.get(0).getAmount());
        assertEquals("EUR", transactions.get(0).getCurrency());
        assertEquals("INCOME", transactions.get(0).getType());

        fakePdf.delete();
    }

    @Test
    void processDocumentWithBank_bankOcrGoodCoverage_returnsRegexResults() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor, parser, bankOcrService, ocrService, llmParser);

        File fakePdf = File.createTempFile("fake-bank-ocr-good-", ".pdf");
        when(textExtractor.isTextBased(fakePdf)).thenReturn(false);
        when(bankOcrService.extractText(fakePdf, "ING")).thenReturn("17/03/2025 Lidl 55.00");

        List<Transaction> transactions = service.processDocument(fakePdf, "ING");

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals("Lidl", transactions.get(0).getDescription());
        verify(bankOcrService, times(1)).extractText(fakePdf, "ING");
        verify(ocrService, never()).extractTextFromPdf(any());

        fakePdf.delete();
    }

    @Test
    void processDocumentWithBank_bankOcrLowCoverage_triggersLlmFallback() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor, parser, bankOcrService, ocrService, llmParser);

        File fakePdf = File.createTempFile("fake-bank-ocr-low-", ".pdf");
        String longText = "17/03/2025 Lidl 55.00\n" + "Padding text to make sure this is well above five hundred characters total. ".repeat(10);
        when(textExtractor.isTextBased(fakePdf)).thenReturn(false);
        when(bankOcrService.extractText(fakePdf, "ING")).thenReturn(longText);

        BankStatementLlmParser.ParsedTransaction pt = new BankStatementLlmParser.ParsedTransaction();
        pt.setDate("18/03/2025");
        pt.setDescription("Bank LLM Desc");
        pt.setAmount(300.00);
        pt.setCurrency("USD");
        pt.setType("TRANSFER");
        when(llmParser.parse(longText)).thenReturn(List.of(pt));

        List<Transaction> transactions = service.processDocument(fakePdf, "ING");

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals("Bank LLM Desc", transactions.get(0).getDescription());
        assertEquals(300.00, transactions.get(0).getAmount());
        assertEquals("USD", transactions.get(0).getCurrency());
        assertEquals("TRANSFER", transactions.get(0).getType());

        fakePdf.delete();
    }

    @Test
    void processDocumentWithBank_genericOcrGoodCoverage_returnsRegexResults() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor, parser, bankOcrService, ocrService, llmParser);

        File fakePdf = File.createTempFile("fake-generic-ocr-good-", ".pdf");
        when(textExtractor.isTextBased(fakePdf)).thenReturn(false);
        when(bankOcrService.extractText(fakePdf, "BT")).thenThrow(new RuntimeException("Bank OCR failed"));
        when(ocrService.extractTextFromPdf(fakePdf)).thenReturn("19/03/2025 Kaufland 77.77");

        List<Transaction> transactions = service.processDocument(fakePdf, "BT");

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals("Kaufland", transactions.get(0).getDescription());

        fakePdf.delete();
    }

    @Test
    void processDocumentWithBank_genericOcrLowCoverage_triggersLlmFallback() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor, parser, bankOcrService, ocrService, llmParser);

        File fakePdf = File.createTempFile("fake-generic-ocr-low-", ".pdf");
        String longText = "19/03/2025 Kaufland 77.77\n" + "Even more padding to exceed the five hundred character limit for low coverage. ".repeat(10);
        when(textExtractor.isTextBased(fakePdf)).thenReturn(false);
        when(bankOcrService.extractText(fakePdf, "BT")).thenThrow(new RuntimeException("Bank OCR failed"));
        when(ocrService.extractTextFromPdf(fakePdf)).thenReturn(longText);

        BankStatementLlmParser.ParsedTransaction pt = new BankStatementLlmParser.ParsedTransaction();
        pt.setDate("20/03/2025");
        pt.setDescription("Generic LLM Desc");
        pt.setAmount(444.44);
        pt.setCurrency("RON");
        pt.setType("EXPENSE");
        when(llmParser.parse(longText)).thenReturn(List.of(pt));

        List<Transaction> transactions = service.processDocument(fakePdf, "BT");

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals("Generic LLM Desc", transactions.get(0).getDescription());
        assertEquals(444.44, transactions.get(0).getAmount());

        fakePdf.delete();
    }

    @Test
    void processDocument_fallbackToLlmBlankText_returnsEmptyList() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor, parser, bankOcrService, ocrService, llmParser);

        File fakePdf = File.createTempFile("fake-blank-", ".pdf");
        when(textExtractor.isTextBased(fakePdf)).thenReturn(false);
        when(ocrService.extractTextFromPdf(fakePdf)).thenReturn("   ");

        List<Transaction> transactions = service.processDocument(fakePdf);

        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());

        fakePdf.delete();
    }

    @Test
    void processDocument_fallbackToLlmParsesMultipleDateFormats() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();
        BankOcrService bankOcrService = mock(BankOcrService.class);
        OcrService ocrService = mock(OcrService.class);
        BankStatementLlmParser llmParser = mock(BankStatementLlmParser.class);

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor, parser, bankOcrService, ocrService, llmParser);

        File fakePdf = File.createTempFile("fake-dates-", ".pdf");
        String longText = "21/03/2025 Test 10.00\n" + "More text to exceed five hundred characters easily for this test. ".repeat(10);
        when(textExtractor.isTextBased(fakePdf)).thenReturn(true);
        when(textExtractor.extractText(fakePdf)).thenReturn(longText);

        BankStatementLlmParser.ParsedTransaction pt1 = new BankStatementLlmParser.ParsedTransaction();
        pt1.setDate("22.03.2025");
        pt1.setDescription("Dot Date");
        pt1.setAmount(11.00);
        pt1.setCurrency("RON");
        pt1.setType("EXPENSE");

        BankStatementLlmParser.ParsedTransaction pt2 = new BankStatementLlmParser.ParsedTransaction();
        pt2.setDate("2025-03-23");
        pt2.setDescription("Dash Date");
        pt2.setAmount(12.00);
        pt2.setCurrency("EUR");
        pt2.setType("INCOME");

        BankStatementLlmParser.ParsedTransaction pt3 = new BankStatementLlmParser.ParsedTransaction();
        pt3.setDate("invalid");
        pt3.setDescription("Invalid Date");
        pt3.setAmount(13.00);
        pt3.setCurrency("USD");
        pt3.setType("TRANSFER");

        BankStatementLlmParser.ParsedTransaction pt4 = new BankStatementLlmParser.ParsedTransaction();
        pt4.setDate(null);
        pt4.setDescription("Null Date");
        pt4.setAmount(14.00);
        pt4.setCurrency("GBP");
        pt4.setType("EXPENSE");

        when(llmParser.parse(longText)).thenReturn(List.of(pt1, pt2, pt3, pt4));

        List<Transaction> transactions = service.processDocument(fakePdf);

        assertNotNull(transactions);
        assertEquals(4, transactions.size());
        assertEquals(LocalDate.of(2025, 3, 22), transactions.get(0).getDate());
        assertEquals(LocalDate.of(2025, 3, 23), transactions.get(1).getDate());
        assertNull(transactions.get(2).getDate());
        assertNull(transactions.get(3).getDate());

        fakePdf.delete();
    }
}
