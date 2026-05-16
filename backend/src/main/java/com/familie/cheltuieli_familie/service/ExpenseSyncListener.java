package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.event.ExpenseSyncEvent;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.ResourceAccessException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseSyncListener {

    private final QdrantVectorService qdrantVectorService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Retryable(
            retryFor = {ResourceAccessException.class, java.net.ConnectException.class, java.net.SocketTimeoutException.class},
            maxAttempts = 4,
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 15000)
    )
    public void handleExpenseSync(ExpenseSyncEvent event) {
        ExpenseEntity expense = event.getExpense();
        log.info("Received sync event for expense ID: {} (attempt will retry on failure)", expense.getId());
        qdrantVectorService.storeExpense(expense);
        log.info("Successfully synced expense ID: {} to vector store", expense.getId());
    }

    @Recover
    public void recover(Exception e, ExpenseSyncEvent event) {
        ExpenseEntity expense = event.getExpense();
        log.error("CRITICAL: All retries exhausted for expense ID: {}. Embedding permanently lost. Error: {}",
                expense.getId(), e.getMessage(), e);
        // TODO: Send to dead-letter queue or alert monitoring
    }
}
