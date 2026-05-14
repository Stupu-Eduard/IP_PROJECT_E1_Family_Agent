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
    void saveShouldStoreMultipleValidTransactions() {
        Transaction first = new Transaction(
                LocalDate.of(2025, 3, 10),
                "Lidl",
                100.5,
                "RON",
                "EXPENSE"
        );

        Transaction second = new Transaction(
                LocalDate.of(2025, 3, 11),
                "Salary",
                3500,
                "EUR",
                "INCOME"
        );

        StorageResult result = storageManager.save(List.of(first, second));

        assertEquals(2, result.getTotalTransactions());
        assertEquals(2, result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());

        verify(repository, times(2)).save(any(ExpenseOCREntity.class));
    }

    @Test
    void saveShouldNotStoreInvalidTransactionWithEmptyDescriptionAndZeroAmount() {
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
    void saveShouldMarkNullTransactionAsFailed() {
        List<Transaction> transactions = new java.util.ArrayList<>();
        transactions.add(null);

        StorageResult result = storageManager.save(transactions);

        assertEquals(1, result.getTotalTransactions());
        assertEquals(0, result.getSavedTransactions());
        assertEquals(1, result.getFailedTransactions());

        verify(repository, never()).save(any(ExpenseOCREntity.class));
    }

    @Test
    void saveShouldMarkTransactionWithNegativeAmountAsFailed() {
        Transaction transaction = new Transaction(
                LocalDate.of(2025, 3, 10),
                "Invalid negative amount",
                -100,
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
    void saveShouldMarkTransactionWithBlankDescriptionAsFailed() {
        Transaction transaction = new Transaction(
                LocalDate.of(2025, 3, 10),
                "   ",
                100,
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
    void saveShouldReturnZeroResultForNullList() {
        StorageResult result = storageManager.save(null);

        assertEquals(0, result.getTotalTransactions());
        assertEquals(0, result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());

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
    void saveShouldCountRepositoryExceptionAsFailedTransaction() {
        Transaction transaction = new Transaction(
                LocalDate.of(2025, 3, 10),
                "Lidl",
                100.5,
                "RON",
                "EXPENSE"
        );

        when(repository.save(any(ExpenseOCREntity.class)))
                .thenThrow(new RuntimeException("DB error"));

        StorageResult result = storageManager.save(List.of(transaction));

        assertEquals(1, result.getTotalTransactions());
        assertEquals(0, result.getSavedTransactions());
        assertEquals(1, result.getFailedTransactions());

        verify(repository, times(1)).save(any(ExpenseOCREntity.class));
    }

    @Test
    void saveShouldContinueAfterOneRepositoryFailure() {
        Transaction first = new Transaction(
                LocalDate.of(2025, 3, 10),
                "Lidl",
                100.5,
                "RON",
                "EXPENSE"
        );

        Transaction second = new Transaction(
                LocalDate.of(2025, 3, 11),
                "Netflix",
                59.99,
                "EUR",
                "EXPENSE"
        );

        when(repository.save(any(ExpenseOCREntity.class)))
                .thenThrow(new RuntimeException("DB error"))
                .thenReturn(new ExpenseOCREntity());

        StorageResult result = storageManager.save(List.of(first, second));

        assertEquals(2, result.getTotalTransactions());
        assertEquals(1, result.getSavedTransactions());
        assertEquals(1, result.getFailedTransactions());

        verify(repository, times(2)).save(any(ExpenseOCREntity.class));
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

    @Test
    void saveShouldUseDefaultDateCurrencyAndTypeWhenMissing() {
        Transaction transaction = new Transaction(
                null,
                "Lidl",
                100.5,
                null,
                null
        );

        storageManager.save(List.of(transaction));

        ArgumentCaptor<ExpenseOCREntity> captor = ArgumentCaptor.forClass(ExpenseOCREntity.class);
        verify(repository).save(captor.capture());

        ExpenseOCREntity savedEntity = captor.getValue();

        assertNotNull(savedEntity.getDate());
        assertEquals(BigDecimal.valueOf(100.5), savedEntity.getAmount());
        assertEquals("Lidl", savedEntity.getDescription());
        assertEquals("EXPENSE", savedEntity.getTransactionType());
        assertEquals("RON", savedEntity.getCurrency());
        assertEquals("OCR", savedEntity.getSourceType());
    }

    @Test
    void saveShouldCountMixedValidAndInvalidTransactionsCorrectly() {
        Transaction valid = new Transaction(
                LocalDate.of(2025, 3, 10),
                "Lidl",
                100.5,
                "RON",
                "EXPENSE"
        );

        Transaction invalidAmount = new Transaction(
                LocalDate.of(2025, 3, 11),
                "Invalid",
                0,
                "RON",
                "EXPENSE"
        );

        Transaction invalidDescription = new Transaction(
                LocalDate.of(2025, 3, 12),
                "",
                50,
                "RON",
                "EXPENSE"
        );

        StorageResult result = storageManager.save(List.of(valid, invalidAmount, invalidDescription));

        assertEquals(3, result.getTotalTransactions());
        assertEquals(1, result.getSavedTransactions());
        assertEquals(2, result.getFailedTransactions());

        verify(repository, times(1)).save(any(ExpenseOCREntity.class));
    }
}