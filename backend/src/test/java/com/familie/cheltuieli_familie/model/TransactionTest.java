package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void gettersAndSettersShouldWork() {
        Transaction transaction = new Transaction();

        transaction.setDate("2025-03-10");
        transaction.setAmount(100.5);
        transaction.setDescription("Lidl");
        transaction.setType("expense");
        transaction.setCurrency("RON");

        assertEquals("2025-03-10", transaction.getDate());
        assertEquals(100.5, transaction.getAmount());
        assertEquals("Lidl", transaction.getDescription());
        assertEquals("expense", transaction.getType());
        assertEquals("RON", transaction.getCurrency());
    }

    @Test
    void constructorShouldSetFields() {
        Transaction transaction = new Transaction("2025-03-10", 100.5, "Lidl", "expense", "RON");

        assertEquals("2025-03-10", transaction.getDate());
        assertEquals(100.5, transaction.getAmount());
        assertEquals("Lidl", transaction.getDescription());
        assertEquals("expense", transaction.getType());
        assertEquals("RON", transaction.getCurrency());
    }

    @Test
    void toStringShouldContainTransactionData() {
        Transaction transaction = new Transaction("2025-03-10", 100.5, "Lidl", "expense", "RON");

        String result = transaction.toString();

        assertTrue(result.contains("2025-03-10"));
        assertTrue(result.contains("100.5"));
        assertTrue(result.contains("Lidl"));
        assertTrue(result.contains("expense"));
        assertTrue(result.contains("RON"));
    }
}
