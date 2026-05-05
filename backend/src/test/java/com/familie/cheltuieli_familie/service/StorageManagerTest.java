package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.entity.TransactionEntity;
import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.repository.TransactionRepository;
import com.familie.cheltuieli_familie.validation.TransactionValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageManagerTest {

    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final TransactionValidator validator = new TransactionValidator();
    private final StorageManager storageManager = new StorageManager(repository, validator);

    @Test
    void saveShouldStoreValidTransactions() {
        Transaction transaction = new Transaction("2025-03-10", 100.5, "Lidl", "expense", "RON");

        StorageResult result = storageManager.save(List.of(transaction));

        assertEquals(1, result.getTotalTransactions());
        assertEquals(1, result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());

        verify(repository, times(1)).save(any(TransactionEntity.class));
    }

    @Test
    void saveShouldNotStoreInvalidTransactions() {
        Transaction transaction = new Transaction("", 0, "", "expense", "RON");

        StorageResult result = storageManager.save(List.of(transaction));

        assertEquals(1, result.getTotalTransactions());
        assertEquals(0, result.getSavedTransactions());
        assertEquals(1, result.getFailedTransactions());

        verify(repository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void saveShouldReturnZeroResultForEmptyList() {
        StorageResult result = storageManager.save(List.of());

        assertEquals(0, result.getTotalTransactions());
        assertEquals(0, result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());

        verify(repository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void saveShouldMapTransactionToEntityCorrectly() {
        Transaction transaction = new Transaction("2025-03-10", 100.5, "Lidl", "expense", "RON");

        storageManager.save(List.of(transaction));

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(repository).save(captor.capture());

        TransactionEntity savedEntity = captor.getValue();

        assertEquals("2025-03-10", savedEntity.getDate());
        assertEquals(100.5, savedEntity.getAmount());
        assertEquals("Lidl", savedEntity.getDescription());
        assertEquals("expense", savedEntity.getType());
        assertEquals("RON", savedEntity.getCurrency());
    }
}
