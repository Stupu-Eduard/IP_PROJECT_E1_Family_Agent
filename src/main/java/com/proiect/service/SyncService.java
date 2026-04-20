package com.proiect.service;

import com.proiect.model.ExpenseEntity;
import com.proiect.repository.ExpenseJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for synchronizing expenses across standard database and vector store.
 */
@Service
public class SyncService {

    @Autowired
    private ExpenseJpaRepository jpaRepository;

    @Autowired
    private QdrantVectorService qdrantVectorService;

    /**
     * Synchronizes the given expense by saving it to the database and vector store.
     * 
     * @param entity The expense entity to sync.
     * @return The saved entity.
     */
    @Transactional
    public ExpenseEntity syncExpense(ExpenseEntity entity) {
        // 1. Save to ExpenseJpaRepository
        ExpenseEntity savedEntity = jpaRepository.save(entity);

        // 2. Save to Vector Store via QdrantVectorService
        // It uses rawInput or category internally. We ensure rawInput is set.
        if (savedEntity.getRawInput() == null && savedEntity.getCategory() != null) {
            savedEntity.setRawInput(savedEntity.getCategory());
        }
        qdrantVectorService.storeExpense(savedEntity);

        return savedEntity;
    }
}
