package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.entity.ExpenseOCREntity;
import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.repository.ExpenseOCRRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
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
                10/03/2026 Lidl Flow Test 100.50 RON
                11/03/2026 Netflix Flow Test 59.99 EUR
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertNotNull(transactions);
        assertEquals(2, transactions.size());

        StorageResult result = storageManager.save(transactions);

        assertEquals(2, result.getTotalTransactions());
        assertEquals(2, result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());

        ArgumentCaptor<ExpenseOCREntity> captor = ArgumentCaptor.forClass(ExpenseOCREntity.class);
        verify(repository, times(2)).save(captor.capture());

        List<ExpenseOCREntity> savedEntities = captor.getAllValues();

        assertEquals("Lidl Flow Test", savedEntities.get(0).getDescription());
        assertEquals(0, new BigDecimal("100.50").compareTo(savedEntities.get(0).getAmount()));
        assertEquals("RON", savedEntities.get(0).getCurrency());
        assertEquals("EXPENSE", savedEntities.get(0).getTransactionType());
        assertEquals("OCR", savedEntities.get(0).getSourceType());

        assertEquals("Netflix Flow Test", savedEntities.get(1).getDescription());
        assertEquals(0, new BigDecimal("59.99").compareTo(savedEntities.get(1).getAmount()));
        assertEquals("EUR", savedEntities.get(1).getCurrency());
        assertEquals("EXPENSE", savedEntities.get(1).getTransactionType());
        assertEquals("OCR", savedEntities.get(1).getSourceType());
    }

    @Test
    void parserOutputWithIncomeAndTransferShouldBeSavedByStorageManager() {
        String rawText = """
                10/03/2026 Incasare salariu Flow Test 3500 RON
                11/03/2026 Virament economii Flow Test 500 EUR
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertNotNull(transactions);
        assertEquals(2, transactions.size());

        StorageResult result = storageManager.save(transactions);

        assertEquals(2, result.getTotalTransactions());
        assertEquals(2, result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());

        ArgumentCaptor<ExpenseOCREntity> captor = ArgumentCaptor.forClass(ExpenseOCREntity.class);
        verify(repository, times(2)).save(captor.capture());

        List<ExpenseOCREntity> savedEntities = captor.getAllValues();

        assertEquals("INCOME", savedEntities.get(0).getTransactionType());
        assertEquals("TRANSFER", savedEntities.get(1).getTransactionType());
        assertEquals("RON", savedEntities.get(0).getCurrency());
        assertEquals("EUR", savedEntities.get(1).getCurrency());
    }

    @Test
    void parserShouldIgnoreInvalidLinesAndStorageShouldSaveOnlyValidTransactions() {
        String rawText = """
                Extras de cont
                10/03/2026 Lidl Valid Flow Test 100.50 RON
                99/99/2026 Linie invalida 50 RON
                11/03/2026 X 10 RON
                12/03/2026 Netflix Valid Flow Test 59.99 EUR
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertNotNull(transactions);
        assertEquals(2, transactions.size());

        StorageResult result = storageManager.save(transactions);

        assertEquals(2, result.getTotalTransactions());
        assertEquals(2, result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());

        verify(repository, times(2)).save(any(ExpenseOCREntity.class));
    }

    @Test
    void parserShouldRemoveDuplicatesBeforeStorageManagerSavesTransactions() {
        String rawText = """
                10/03/2026 Lidl Duplicate Flow Test 100.50 RON
                10/03/2026 Lidl Duplicate Flow Test 100.50 RON
                11/03/2026 Netflix Duplicate Flow Test 59.99 EUR
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        assertEquals(2, transactions.size());

        StorageResult result = storageManager.save(transactions);

        assertEquals(2, result.getTotalTransactions());
        assertEquals(2, result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());

        verify(repository, times(2)).save(any(ExpenseOCREntity.class));
    }

    @Test
    void storageManagerShouldReportFailuresWhenRepositoryThrowsException() {
        String rawText = """
                10/03/2026 Lidl Failure Test 100.50 RON
                11/03/2026 Netflix Failure Test 59.99 EUR
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        doThrow(new RuntimeException("DB error"))
                .when(repository)
                .save(any(ExpenseOCREntity.class));

        StorageResult result = storageManager.save(transactions);

        assertEquals(2, result.getTotalTransactions());
        assertEquals(0, result.getSavedTransactions());
        assertEquals(2, result.getFailedTransactions());

        verify(repository, times(2)).save(any(ExpenseOCREntity.class));
    }

    @Test
    void storageManagerShouldReportPartialSuccessWhenOnlyOneSaveFails() {
        String rawText = """
                10/03/2026 Lidl Partial Flow Test 100.50 RON
                11/03/2026 Netflix Partial Flow Test 59.99 EUR
                """;

        List<Transaction> transactions = parser.parseText(rawText);

        when(repository.save(any(ExpenseOCREntity.class)))
                .thenThrow(new RuntimeException("DB error"))
                .thenReturn(new ExpenseOCREntity());

        StorageResult result = storageManager.save(transactions);

        assertEquals(2, result.getTotalTransactions());
        assertEquals(1, result.getSavedTransactions());
        assertEquals(1, result.getFailedTransactions());

        verify(repository, times(2)).save(any(ExpenseOCREntity.class));
    }
}
