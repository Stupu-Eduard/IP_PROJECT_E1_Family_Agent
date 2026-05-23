package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void gettersAndSettersShouldWork() {
        Transaction transaction = new Transaction();

        transaction.setDate(LocalDate.of(2025, 3, 10));
        transaction.setAmount(100.5);
        transaction.setDescription("Lidl");
        transaction.setType("EXPENSE");
        transaction.setCurrency("RON");

        assertEquals(LocalDate.of(2025, 3, 10), transaction.getDate());
        assertEquals(100.5, transaction.getAmount());
        assertEquals("Lidl", transaction.getDescription());
        assertEquals("EXPENSE", transaction.getType());
        assertEquals("RON", transaction.getCurrency());
    }

    @Test
    void constructorWithThreeArgumentsShouldSetDefaultCurrencyAndType() {
        Transaction transaction = new Transaction(
                LocalDate.of(2025, 3, 10),
                "Lidl",
                100.5
        );

        assertEquals(LocalDate.of(2025, 3, 10), transaction.getDate());
        assertEquals(100.5, transaction.getAmount());
        assertEquals("Lidl", transaction.getDescription());
        assertEquals("RON", transaction.getCurrency());
        assertEquals("EXPENSE", transaction.getType());
    }

    @Test
    void constructorWithFiveArgumentsShouldSetAllFields() {
        Transaction transaction = new Transaction(
                LocalDate.of(2025, 3, 10),
                "Lidl",
                100.5,
                "EUR",
                "TRANSFER"
        );

        assertEquals(LocalDate.of(2025, 3, 10), transaction.getDate());
        assertEquals(100.5, transaction.getAmount());
        assertEquals("Lidl", transaction.getDescription());
        assertEquals("EUR", transaction.getCurrency());
        assertEquals("TRANSFER", transaction.getType());
    }

    @Test
    void constructorWithSixArgumentsShouldSetReceiptUrl() {
        Transaction transaction = new Transaction(
                LocalDate.of(2025, 3, 10),
                "Lidl",
                100.5,
                "RON",
                "EXPENSE",
                "https://cloudinary.com/receipt.jpg"
        );

        assertEquals("https://cloudinary.com/receipt.jpg", transaction.getReceiptUrl());
        assertEquals(LocalDate.of(2025, 3, 10), transaction.getDate());
        assertEquals(100.5, transaction.getAmount());
    }

    @Test
    void setReceiptUrlShouldUpdateValue() {
        Transaction transaction = new Transaction();
        transaction.setReceiptUrl("https://example.com/img.jpg");
        assertEquals("https://example.com/img.jpg", transaction.getReceiptUrl());
        transaction.setReceiptUrl(null);
        assertNull(transaction.getReceiptUrl());
    }

    @Test
    void toStringShouldContainTransactionData() {
        Transaction transaction = new Transaction(
                LocalDate.of(2025, 3, 10),
                "Lidl",
                100.5,
                "RON",
                "EXPENSE"
        );

        String result = transaction.toString();

        assertTrue(result.contains("2025-03-10"));
        assertTrue(result.contains("100.5"));
        assertTrue(result.contains("Lidl"));
        assertTrue(result.contains("RON"));
        assertTrue(result.contains("EXPENSE"));
    }
}