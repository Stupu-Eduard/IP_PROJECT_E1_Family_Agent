package com.proiect.service;

import com.proiect.exception.PipelineException;
import com.proiect.repository.ExpenseJpaRepository;
import com.proiect.repository.ExpenseVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineValidationService {

    private final ExpenseJpaRepository repository;
    private final ExpenseVectorRepository vectorRepository;

    public void validatePersistence(Long id, float[] vector) {
        log.info("Validating persistence for ID: {}", id);
        
        if (!repository.existsById(id)) {
            throw new PipelineException("Entity not found in SQL Database after save!");
        }

        if (!vectorRepository.existsInVectorStore(id)) {
            throw new PipelineException("Entity not found in Qdrant Vector Store!");
        }

        log.info("Validation successful for ID: {}", id);
    }
}
