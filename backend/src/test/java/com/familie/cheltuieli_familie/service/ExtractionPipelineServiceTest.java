package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtractionPipelineServiceTest {

    @Test
    void processDocumentShouldExtractTextAndParseTransactions() throws Exception {
        TextBasedPdfExtractor textExtractor = mock(TextBasedPdfExtractor.class);
        BankStatementParser parser = new BankStatementParser();

        ExtractionPipelineService service = new ExtractionPipelineService(
                textExtractor,
                parser
        );

        File fakePdf = File.createTempFile("fake-text-pdf-", ".pdf");

        when(textExtractor.extractText(fakePdf)).thenReturn("""
                10/03/2025 Lidl 100.50
                11/03/2025 Netflix 59.99
                """);

        List<Transaction> transactions = service.processDocument(fakePdf, "unknown");

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
}