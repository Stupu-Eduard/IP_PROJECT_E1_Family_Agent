package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.event.ExpenseSyncEvent;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseSyncListenerTest {

    @Mock
    private QdrantVectorService qdrantVectorService;

    @InjectMocks
    private ExpenseSyncListener expenseSyncListener;

    @Test
    void testHandleExpenseSync() {
        ExpenseEntity expense = ExpenseEntity.builder().id(1L).build();
        ExpenseSyncEvent event = new ExpenseSyncEvent(this, expense);

        expenseSyncListener.handleExpenseSync(event);

        verify(qdrantVectorService, times(1)).storeExpense(expense);
    }

    @Test
    void testHandleExpenseSyncWithException() {
        ExpenseEntity expense = ExpenseEntity.builder().id(2L).build();
        ExpenseSyncEvent event = new ExpenseSyncEvent(this, expense);

        doThrow(new RuntimeException("Qdrant down")).when(qdrantVectorService).storeExpense(expense);

        // With @Retryable, exceptions propagate up for retry handling
        assertThrows(RuntimeException.class, () -> expenseSyncListener.handleExpenseSync(event));

        verify(qdrantVectorService, times(1)).storeExpense(expense);
    }

    @Test
    void testRecover() {
        ExpenseEntity expense = ExpenseEntity.builder().id(3L).build();
        ExpenseSyncEvent event = new ExpenseSyncEvent(this, expense);
        Exception exception = new RuntimeException("All retries failed");

        assertDoesNotThrow(() -> expenseSyncListener.recover(exception, event));
    }
}
