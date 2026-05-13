package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.event.ExpenseSyncEvent;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseSyncListener {

    private final QdrantVectorService qdrantVectorService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleExpenseSync(ExpenseSyncEvent event) {
        ExpenseEntity expense = event.getExpense();
        log.info("Received sync event for expense ID: {}", expense.getId());
        try {
            qdrantVectorService.storeExpense(expense);
            log.info("Successfully synced expense ID: {} to vector store", expense.getId());
        } catch (Exception e) {
            log.error("Failed to sync expense ID: {} to vector store", expense.getId(), e);
        }
    }
}
