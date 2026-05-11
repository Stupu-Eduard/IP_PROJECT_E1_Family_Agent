package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BankStatementParserTest {

    private final BankStatementParser parser = new BankStatementParser();

    @Test
    void parseTextShouldExtractTransactionWithDefaultCurrencyAndType() {
        String rawText = """
                10/03/2026 Lidl 100.50
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertNotNull(transactions);
        assertEquals(1, transactions.size());

        Transaction transaction = transactions.get(0);

        assertEquals(LocalDate.of(2026, 3, 10), transaction.getDate());
        assertEquals("Lidl", transaction.getDescription());
        assertEquals(100.50, transaction.getAmount());
        assertEquals("RON", transaction.getCurrency());
        assertEquals("EXPENSE", transaction.getType());
    }

    @Test
    void parseTextShouldExtractMultipleTransactionsWithCurrencies() {
        String rawText = """
                10/03/2026 Lidl 100.50 RON
                11/03/2026 Netflix 59.99 EUR
                12/03/2026 Amazon 25,75 USD
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertEquals(3, transactions.size());

        assertEquals(LocalDate.of(2026, 3, 10), transactions.get(0).getDate());
        assertEquals("Lidl", transactions.get(0).getDescription());
        assertEquals(100.50, transactions.get(0).getAmount());
        assertEquals("RON", transactions.get(0).getCurrency());
        assertEquals("EXPENSE", transactions.get(0).getType());

        assertEquals(LocalDate.of(2026, 3, 11), transactions.get(1).getDate());
        assertEquals("Netflix", transactions.get(1).getDescription());
        assertEquals(59.99, transactions.get(1).getAmount());
        assertEquals("EUR", transactions.get(1).getCurrency());
        assertEquals("EXPENSE", transactions.get(1).getType());

        assertEquals(LocalDate.of(2026, 3, 12), transactions.get(2).getDate());
        assertEquals("Amazon", transactions.get(2).getDescription());
        assertEquals(25.75, transactions.get(2).getAmount());
        assertEquals("USD", transactions.get(2).getCurrency());
        assertEquals("EXPENSE", transactions.get(2).getType());
    }

    @Test
    void parseTextShouldExtractTransactionTypeFromKeywords() {
        String rawText = """
                10/03/2026 Salariu martie 3500 RON
                11/03/2026 Transfer cont economii 500 RON
                12/03/2026 Plata card Lidl 100.50 RON
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertEquals(3, transactions.size());

        assertEquals("INCOME", transactions.get(0).getType());
        assertEquals("TRANSFER", transactions.get(1).getType());
        assertEquals("EXPENSE", transactions.get(2).getType());
    }

    @Test
    void parseTextShouldExtractIncomeAndTransferTypes() {
        String rawText = """
                10/03/2026 Incasare salariu martie 3500 RON
                11/03/2026 Virament cont economii 500 RON
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertEquals(2, transactions.size());

        assertEquals("INCOME", transactions.get(0).getType());
        assertEquals("TRANSFER", transactions.get(1).getType());
    }

    @Test
    void parseTextShouldExtractSupportedCurrencies() {
        String rawText = """
                10/03/2026 Plata card Lidl 100.50 RON
                11/03/2026 Netflix subscription 59.99 EUR
                12/03/2026 Amazon order 25.75 USD
                13/03/2026 London payment 30.25 GBP
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertEquals(4, transactions.size());

        assertEquals("RON", transactions.get(0).getCurrency());
        assertEquals("EUR", transactions.get(1).getCurrency());
        assertEquals("USD", transactions.get(2).getCurrency());
        assertEquals("GBP", transactions.get(3).getCurrency());
    }

    @Test
    void parseTextShouldUseDefaultsWhenCurrencyAndTypeAreMissing() {
        String rawText = """
                10/03/2026 Magazin alimentar 80.25
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertEquals(1, transactions.size());
        assertEquals("RON", transactions.get(0).getCurrency());
        assertEquals("EXPENSE", transactions.get(0).getType());
    }

    @Test
    void parseTextShouldHandleOcrMistakeR0NAsRon() {
        String rawText = """
                10/03/2026 Lidl OCR Test 100.50 R0N
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertEquals(1, transactions.size());
        assertEquals("RON", transactions.get(0).getCurrency());
        assertEquals("Lidl OCR Test", transactions.get(0).getDescription());
    }

    @Test
    void parseTextShouldIgnoreDuplicateTransactions() {
        String rawText = """
                10/03/2026 Lidl 100.50 RON
                10/03/2026 Lidl 100.50 RON
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertEquals(1, transactions.size());
        assertEquals("Lidl", transactions.get(0).getDescription());
    }

    @Test
    void parseTextShouldKeepDifferentTransactionsWithSameDate() {
        String rawText = """
                10/03/2026 Lidl 100.50 RON
                10/03/2026 Netflix 59.99 RON
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertEquals(2, transactions.size());
        assertEquals("Lidl", transactions.get(0).getDescription());
        assertEquals("Netflix", transactions.get(1).getDescription());
    }

    @Test
    void parseTextShouldHandleCommaAmountsAndExtraSpaces() {
        String rawText = """
                10/03/2026      Kaufland      45,99      RON
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertEquals(1, transactions.size());
        assertEquals("Kaufland", transactions.get(0).getDescription());
        assertEquals(45.99, transactions.get(0).getAmount());
        assertEquals("RON", transactions.get(0).getCurrency());
    }

    @Test
    void parseTextShouldIgnoreInvalidLines() {
        String rawText = """
                Extras de cont
                Lidl 100.50
                Total 100.50
                99/99/2026 Linie cu data invalida 10 RON
                10/03/2026 12345 100 RON
                10/03/2026 X 100 RON
                10/03/2026 Descriere fara suma
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }

    @Test
    void parseTextShouldIgnoreInvalidDescriptionsAndMissingAmounts() {
        String rawText = """
            10/03/2026 X 50 RON
            11/03/2026 123456 70 RON
            12/03/2026 Descriere fara suma
            """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertTrue(transactions.isEmpty());
    }

    @Test
    void parseTextShouldReturnEmptyListForNullText() {
        List<Transaction> transactions = parser.parseText(null);

        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }

    @Test
    void parseTextShouldReturnEmptyListForBlankText() {
        List<Transaction> transactions = parser.parseText("   \n   \t ");

        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }
}
