package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.Location;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorStoreSyncServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private QdrantVectorService qdrantVectorService;

    @InjectMocks
    private VectorStoreSyncService vectorStoreSyncService;

    @Test
    void testSyncMissingExpenses() {
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setAmount(new BigDecimal("100.00"));
        expense.setDescription("Test expense");
        expense.setExpenseDate(LocalDateTime.now());
        expense.setSourceType("MANUAL");
        expense.setRawInput("Raw input text");

        when(expenseRepository.findAll()).thenReturn(List.of(expense));
        when(qdrantVectorService.existsInVectorStore(1L)).thenReturn(false);
        doNothing().when(qdrantVectorService).storeExpense(any());

        vectorStoreSyncService.syncMissingExpenses();

        verify(qdrantVectorService).existsInVectorStore(1L);
        verify(qdrantVectorService).storeExpense(any());
    }

    @Test
    void testSyncMissingExpensesAlreadyExists() {
        Expense expense = new Expense();
        expense.setId(2L);
        expense.setAmount(new BigDecimal("50.00"));
        expense.setDescription("Existing expense");
        expense.setExpenseDate(LocalDateTime.now());
        expense.setSourceType("OCR");

        when(expenseRepository.findAll()).thenReturn(List.of(expense));
        when(qdrantVectorService.existsInVectorStore(2L)).thenReturn(true);

        vectorStoreSyncService.syncMissingExpenses();

        verify(qdrantVectorService).existsInVectorStore(2L);
        verify(qdrantVectorService, never()).storeExpense(any());
    }

    @Test
    void testSyncMissingExpensesWithNullRawInput() {
        Expense expense = new Expense();
        expense.setId(3L);
        expense.setAmount(new BigDecimal("75.00"));
        expense.setDescription("No raw input");
        expense.setExpenseDate(LocalDateTime.now());
        expense.setSourceType("MANUAL");
        expense.setRawInput(null);

        Category category = new Category();
        category.setName("Food");
        expense.setCategory(category);

        Location location = new Location();
        location.setStore("Kaufland");
        expense.setLocation(location);

        User user = new User();
        user.setName("Alice");
        expense.setUser(user);

        when(expenseRepository.findAll()).thenReturn(List.of(expense));
        when(qdrantVectorService.existsInVectorStore(3L)).thenReturn(false);
        doNothing().when(qdrantVectorService).storeExpense(any());

        vectorStoreSyncService.syncMissingExpenses();

        verify(qdrantVectorService).storeExpense(any());
    }

    @Test
    void testSyncMissingExpensesWithException() {
        Expense expense = new Expense();
        expense.setId(4L);
        expense.setAmount(new BigDecimal("200.00"));
        expense.setDescription("Error expense");
        expense.setExpenseDate(LocalDateTime.now());
        expense.setSourceType("MANUAL");

        when(expenseRepository.findAll()).thenReturn(List.of(expense));
        when(qdrantVectorService.existsInVectorStore(4L)).thenReturn(false);
        doThrow(new RuntimeException("Qdrant error")).when(qdrantVectorService).storeExpense(any());

        assertDoesNotThrow(() -> vectorStoreSyncService.syncMissingExpenses());

        verify(qdrantVectorService).storeExpense(any());
    }

    @Test
    void testSyncMissingExpensesEmptyList() {
        when(expenseRepository.findAll()).thenReturn(List.of());

        vectorStoreSyncService.syncMissingExpenses();

        verify(qdrantVectorService, never()).existsInVectorStore(any());
        verify(qdrantVectorService, never()).storeExpense(any());
    }
}
