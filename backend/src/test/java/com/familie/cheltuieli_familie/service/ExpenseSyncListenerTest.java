package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.event.ExpenseSyncEvent;
import com.familie.cheltuieli_familie.model.Expense;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseSyncListenerTest {

    @Mock
    private QdrantVectorService qdrantVectorService;

    @InjectMocks
    private ExpenseSyncListener expenseSyncListener;

    @Test
    void testHandleExpenseSync() {
        Expense expense = Expense.builder().id(1L).build();
        ExpenseSyncEvent event = new ExpenseSyncEvent(this, expense);

        expenseSyncListener.handleExpenseSync(event);

        verify(qdrantVectorService, times(1)).storeExpense(expense);
    }

    @Test
    void testHandleExpenseSyncWithException() {
        Expense expense = Expense.builder().id(2L).build();
        ExpenseSyncEvent event = new ExpenseSyncEvent(this, expense);

        doThrow(new RuntimeException("Qdrant down")).when(qdrantVectorService).storeExpense(expense);

        // Should not throw — exception is caught and logged
        expenseSyncListener.handleExpenseSync(event);

        verify(qdrantVectorService, times(1)).storeExpense(expense);
    }
}
