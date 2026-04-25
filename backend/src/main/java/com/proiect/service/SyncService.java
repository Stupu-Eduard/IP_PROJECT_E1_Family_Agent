package com.proiect.service;

import com.proiect.event.ExpenseSyncEvent;
import com.proiect.model.ExpenseEntity;
import com.proiect.repository.ExpenseJpaRepository;
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
    public ExpenseEntity syncExpense(ExpenseEntity entity) {
        log.info("Saving expense to database...");
        ExpenseEntity savedEntity = jpaRepository.save(entity);

        // Ensure rawInput is set for vector store
        if (savedEntity.getRawInput() == null && savedEntity.getCategory() != null) {
            savedEntity.setRawInput(savedEntity.getCategory());
        }

        // Publish event for automatic sync to Qdrant
        log.info("Publishing ExpenseSyncEvent for ID: {}", savedEntity.getId());
        eventPublisher.publishEvent(new ExpenseSyncEvent(this, savedEntity));

        return savedEntity;
    }
}
