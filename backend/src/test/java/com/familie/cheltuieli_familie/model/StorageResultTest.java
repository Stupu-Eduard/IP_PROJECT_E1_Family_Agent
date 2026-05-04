package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageResultTest {

    private StorageResult createStorageResult() {
        return new StorageResult();
    }

    @Test
    void testNoArgsConstructorAndSetters() {
        StorageResult result = createStorageResult();
        result.setTotalTransactions(10);
        result.setSavedTransactions(8);
        result.setFailedTransactions(2);

        assertEquals(10, result.getTotalTransactions());
        assertEquals(8, result.getSavedTransactions());
        assertEquals(2, result.getFailedTransactions());
    }

    @Test
    void testAllArgsConstructorAndGetters() {
        StorageResult result = new StorageResult(10, 8, 2);

        assertEquals(10, result.getTotalTransactions());
        assertEquals(8, result.getSavedTransactions());
        assertEquals(2, result.getFailedTransactions());
    }

    @Test
    void testToString() {
        StorageResult result = new StorageResult(10, 8, 2);
        String expected = "StorageResult{totalTransactions=10, savedTransactions=8, failedTransactions=2}";

        assertEquals(expected, result.toString());
    }
}
