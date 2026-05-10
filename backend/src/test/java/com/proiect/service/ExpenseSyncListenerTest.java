package com.proiect.service;
import org.springframework.test.context.ContextConfiguration;

import com.proiect.event.ExpenseSyncEvent;
import com.proiect.model.ExpenseEntity;
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
        ExpenseEntity expense = ExpenseEntity.builder().id(1L).build();
        ExpenseSyncEvent event = new ExpenseSyncEvent(this, expense);

        expenseSyncListener.handleExpenseSync(event);

        verify(qdrantVectorService, times(1)).storeExpense(expense);
    }
}
