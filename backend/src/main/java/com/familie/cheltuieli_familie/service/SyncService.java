package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.event.ExpenseSyncEvent;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for synchronizing expenses across standard database and vector store.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final ExpenseJpaRepository jpaRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Synchronizes the given expense by saving it to the database and publishing a sync event.
     * 
     * @param entity The expense entity to sync.
     * @return The saved entity.
     */
    @Transactional
    public Expense syncExpense(Expense entity) {
        log.info("Saving expense to database...");
        Expense savedEntity = jpaRepository.save(entity);

        // Ensure rawInput is set for vector store
        if (savedEntity.getRawInput() == null && savedEntity.getAiCategory() != null) {
            savedEntity.setRawInput(savedEntity.getAiCategory());
        }

        // Publish event for automatic sync to Qdrant
        log.info("Publishing ExpenseSyncEvent for ID: {}", savedEntity.getId());
        eventPublisher.publishEvent(new ExpenseSyncEvent(this, savedEntity));

        return savedEntity;
    }
}
