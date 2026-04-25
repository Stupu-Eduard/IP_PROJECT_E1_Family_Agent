package com.proiect.service;

import com.proiect.event.ExpenseSyncEvent;
import com.proiect.model.ExpenseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
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
    @Async // Optional: can be async to not block the main flow if needed
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
