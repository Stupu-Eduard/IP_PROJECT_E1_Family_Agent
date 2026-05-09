package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.entity.ExpenseOCREntity;
import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.repository.ExpenseOCRRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParserStorageFlowTest {

    private final BankStatementParser parser = new BankStatementParser();

    private final ExpenseOCRRepository repository = mock(ExpenseOCRRepository.class);
    private final StorageManager storageManager = new StorageManager(repository);

    @Test
    void parserOutputShouldBeSavedByStorageManager() {
        String rawText = """
                10/03/2025 Lidl 100.50
                11/03/2025 Netflix 59.99
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertNotNull(transactions);
        assertFalse(transactions.isEmpty());
        assertEquals(2, transactions.size());

        StorageResult result = storageManager.save(transactions);

        assertEquals(2, result.getTotalTransactions());
        assertEquals(2, result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());

        verify(repository, times(2)).save(any(ExpenseOCREntity.class));
    }
}