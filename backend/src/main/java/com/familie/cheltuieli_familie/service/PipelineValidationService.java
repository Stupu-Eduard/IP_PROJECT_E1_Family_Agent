package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.exception.PipelineException;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineValidationService {

    private final ExpenseJpaRepository repository;
    private final QdrantVectorService qdrantVectorService;

    public void validatePersistence(Long id) {
        log.info("Validating persistence for ID: {}", id);
        
        if (!repository.existsById(id)) {
            throw new PipelineException("Entity not found in SQL Database after save!");
        }

        // Qdrant vector check is now handled via QdrantVectorService
        boolean inVector = qdrantVectorService.existsInVectorStore(id);
        if (!inVector) {
            log.warn("Entity ID {} not found in Qdrant vector store (non-fatal)", id);
        } else {
            log.info("Entity ID {} confirmed in Qdrant vector store.", id);
        }

        log.info("Validation successful for ID: {}", id);
    }
}
