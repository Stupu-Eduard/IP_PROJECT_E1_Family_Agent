package com.proiect.service;

import com.proiect.model.ExpenseEntity;
import com.proiect.repository.ExpenseJpaRepository;
import com.proiect.repository.ExpenseVectorRepository;
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
    private EmbeddingService embeddingService;

    @Autowired
    private ExpenseVectorRepository vectorRepository;

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

        // 2. Calls EmbeddingService to get the vector
        // Using category as the text for embedding
        String textToEmbed = savedEntity.getCategory() != null ? savedEntity.getCategory() : "";
        float[] vector = embeddingService.getEmbedding(textToEmbed);

        // 3. Saves to ExpenseVectorRepository
        vectorRepository.saveVector(savedEntity, vector);

        return savedEntity;
    }
}
