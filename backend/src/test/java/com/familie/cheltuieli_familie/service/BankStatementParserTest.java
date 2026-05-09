package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BankStatementParserTest {

    private final BankStatementParser parser = new BankStatementParser();

    @Test
    void parseTextShouldExtractTransactionFromBankStatementText() {
        String rawText = """
                10/03/2025 Lidl 100.50
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertNotNull(transactions);
        assertFalse(transactions.isEmpty());

        Transaction transaction = transactions.get(0);

        assertEquals(LocalDate.of(2025, 3, 10), transaction.getDate());
        assertEquals("Lidl", transaction.getDescription());
        assertEquals(100.50, transaction.getAmount());
        assertEquals("RON", transaction.getCurrency());
        assertEquals("EXPENSE", transaction.getType());
    }

    @Test
    void parseTextShouldExtractMultipleTransactions() {
        String rawText = """
                10/03/2025 Lidl 100.50
                11/03/2025 Netflix 59.99
                """;

        List<Transaction> transactions = parser.parseText(rawText);

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
    }

    @Test
    void parseTextShouldReturnEmptyListForNullText() {
        List<Transaction> transactions = parser.parseText(null);

        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }

    @Test
    void parseTextShouldReturnEmptyListForEmptyText() {
        List<Transaction> transactions = parser.parseText("");

        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }

    @Test
    void parseTextShouldIgnoreLinesWithoutDate() {
        String rawText = """
                Extras de cont
                Lidl 100.50
                Total 100.50
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }
}