package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.mapper.ExpenseMapper;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QdrantResyncServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseMapper expenseMapper;

    @Mock
    private QdrantVectorService qdrantVectorService;

    @InjectMocks
    private QdrantResyncService qdrantResyncService;

    @Test
    void testResyncAllExpensesSuccess() {
        List<Expense> expenses = List.of(createExpense(1L), createExpense(2L));
        when(expenseRepository.findAll()).thenReturn(expenses);
        when(expenseMapper.toExpenseEntity(any(Expense.class))).thenReturn(createExpenseEntity(1L));

        QdrantResyncService.ResyncResult result = qdrantResyncService.resyncAllExpenses();

        assertEquals(2, result.processedCount());
        assertEquals(0, result.errorCount());
        verify(qdrantVectorService, times(2)).storeExpense(any(ExpenseEntity.class));
    }

    @Test
    void testResyncAllExpensesHandlesFailures() {
        Expense expense1 = createExpense(1L);
        Expense expense2 = createExpense(2L);
        when(expenseRepository.findAll()).thenReturn(List.of(expense1, expense2));
        when(expenseMapper.toExpenseEntity(expense1)).thenReturn(createExpenseEntity(1L));
        when(expenseMapper.toExpenseEntity(expense2)).thenReturn(createExpenseEntity(2L));
        doThrow(new RuntimeException("Qdrant error")).when(qdrantVectorService).storeExpense(any(ExpenseEntity.class));

        QdrantResyncService.ResyncResult result = qdrantResyncService.resyncAllExpenses();

        assertEquals(0, result.processedCount());
        assertEquals(2, result.errorCount());
    }

    @Test
    void testResyncAllExpensesPartialFailure() {
        Expense expense1 = createExpense(1L);
        Expense expense2 = createExpense(2L);
        when(expenseRepository.findAll()).thenReturn(List.of(expense1, expense2));
        when(expenseMapper.toExpenseEntity(expense1)).thenReturn(createExpenseEntity(1L));
        when(expenseMapper.toExpenseEntity(expense2)).thenReturn(createExpenseEntity(2L));
        doNothing().when(qdrantVectorService).storeExpense(argThat(e -> e.getId().equals(1L)));
        doThrow(new RuntimeException("Qdrant error")).when(qdrantVectorService).storeExpense(argThat(e -> e.getId().equals(2L)));

        QdrantResyncService.ResyncResult result = qdrantResyncService.resyncAllExpenses();

        assertEquals(1, result.processedCount());
        assertEquals(1, result.errorCount());
    }

    @Test
    void testResyncAllExpensesLogsProgress() {
        List<Expense> expenses = new ArrayList<>();
        for (long i = 1; i <= 250; i++) {
            expenses.add(createExpense(i));
        }
        when(expenseRepository.findAll()).thenReturn(expenses);
        when(expenseMapper.toExpenseEntity(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            return createExpenseEntity(e.getId());
        });

        QdrantResyncService.ResyncResult result = qdrantResyncService.resyncAllExpenses();

        assertEquals(250, result.processedCount());
        assertEquals(0, result.errorCount());
        verify(qdrantVectorService, times(250)).storeExpense(any(ExpenseEntity.class));
    }

    @Test
    void testResyncExpensesForFamilySuccess() {
        Long familyId = 10L;
        List<Expense> expenses = List.of(createExpense(1L, familyId), createExpense(2L, familyId));
        when(expenseRepository.findAllByFamilyId(familyId)).thenReturn(expenses);
        when(expenseMapper.toExpenseEntity(any(Expense.class))).thenReturn(createExpenseEntity(1L));

        QdrantResyncService.ResyncResult result = qdrantResyncService.resyncExpensesForFamily(familyId);

        assertEquals(2, result.processedCount());
        assertEquals(0, result.errorCount());
        verify(expenseRepository).findAllByFamilyId(familyId);
        verify(qdrantVectorService, times(2)).storeExpense(any(ExpenseEntity.class));
    }

    @Test
    void testResyncExpensesForFamilyThrowsWhenNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> qdrantResyncService.resyncExpensesForFamily(null));
        assertEquals("familyId must not be null", ex.getMessage());
    }

    @Test
    void testResyncExpensesForFamilyHandlesNullFamilyAndUser() {
        Long familyId = 10L;
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setAmount(new BigDecimal("50.00"));
        expense.setFamily(null);
        expense.setUser(null);
        when(expenseRepository.findAllByFamilyId(familyId)).thenReturn(List.of(expense));
        when(expenseMapper.toExpenseEntity(expense)).thenReturn(
                ExpenseEntity.builder()
                        .id(1L)
                        .amount(new BigDecimal("50.00"))
                        .familyId(null)
                        .userId(null)
                        .build()
        );

        QdrantResyncService.ResyncResult result = qdrantResyncService.resyncExpensesForFamily(familyId);

        assertEquals(1, result.processedCount());
        assertEquals(0, result.errorCount());
    }

    @Test
    void testResyncAllExpensesEmptyList() {
        when(expenseRepository.findAll()).thenReturn(List.of());

        QdrantResyncService.ResyncResult result = qdrantResyncService.resyncAllExpenses();

        assertEquals(0, result.processedCount());
        assertEquals(0, result.errorCount());
        verifyNoInteractions(qdrantVectorService);
    }

    private Expense createExpense(Long id) {
        return createExpense(id, 5L);
    }

    private Expense createExpense(Long id, Long familyId) {
        Expense expense = new Expense();
        expense.setId(id);
        expense.setAmount(new BigDecimal("100.00"));
        expense.setCreatedAt(LocalDateTime.now());

        Family family = new Family();
        family.setId(familyId);
        expense.setFamily(family);

        User user = new User();
        user.setId(99L);
        expense.setUser(user);

        return expense;
    }

    private ExpenseEntity createExpenseEntity(Long id) {
        return ExpenseEntity.builder()
                .id(id)
                .amount(new BigDecimal("100.00"))
                .category("Food")
                .location("Kaufland")
                .person("Ion")
                .rawInput("Test expense " + id)
                .familyId(5L)
                .userId(99L)
                .build();
    }
}
