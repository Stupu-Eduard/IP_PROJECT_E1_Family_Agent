package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.entity.ExpenseOCREntity;
import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.repository.ExpenseOCRRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageManagerTest {

    private final ExpenseOCRRepository repository = mock(ExpenseOCRRepository.class);
    private final StorageManager storageManager = new StorageManager(repository);

    @Test
    void saveShouldStoreValidTransactions() {
        Transaction transaction = new Transaction(
                LocalDate.of(2025, 3, 10),
                "Lidl",
                100.5,
                "RON",
                "EXPENSE"
        );

        StorageResult result = storageManager.save(List.of(transaction));

        assertEquals(1, result.getTotalTransactions());
        assertEquals(1, result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());

        verify(repository, times(1)).save(any(ExpenseOCREntity.class));
    }

    @Test
    void saveShouldNotStoreInvalidTransactions() {
        Transaction transaction = new Transaction(
                null,
                "",
                0,
                "RON",
                "EXPENSE"
        );

        StorageResult result = storageManager.save(List.of(transaction));

        assertEquals(1, result.getTotalTransactions());
        assertEquals(0, result.getSavedTransactions());
        assertEquals(1, result.getFailedTransactions());

        verify(repository, never()).save(any(ExpenseOCREntity.class));
    }

    @Test
    void saveShouldReturnZeroResultForEmptyList() {
        StorageResult result = storageManager.save(List.of());

        assertEquals(0, result.getTotalTransactions());
        assertEquals(0, result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());

        verify(repository, never()).save(any(ExpenseOCREntity.class));
    }

    @Test
    void saveShouldMapTransactionToEntityCorrectly() {
        Transaction transaction = new Transaction(
                LocalDate.of(2025, 3, 10),
                "Lidl",
                100.5,
                "RON",
                "EXPENSE"
        );

        storageManager.save(List.of(transaction));

        ArgumentCaptor<ExpenseOCREntity> captor = ArgumentCaptor.forClass(ExpenseOCREntity.class);
        verify(repository).save(captor.capture());

        ExpenseOCREntity savedEntity = captor.getValue();

        assertEquals(LocalDate.of(2025, 3, 10).atStartOfDay(), savedEntity.getDate());
        assertEquals(BigDecimal.valueOf(100.5), savedEntity.getAmount());
        assertEquals("Lidl", savedEntity.getDescription());
        assertEquals("EXPENSE", savedEntity.getTransactionType());
        assertEquals("RON", savedEntity.getCurrency());
        assertEquals("OCR", savedEntity.getSourceType());
    }
}