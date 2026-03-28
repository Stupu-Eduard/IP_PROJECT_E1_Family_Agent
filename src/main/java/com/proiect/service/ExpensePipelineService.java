package com.proiect.service;

import com.proiect.client.ExtractionClient;
import com.proiect.dto.ExtractedExpenseDTO;
import com.proiect.model.ExpenseEntity;
import com.proiect.repository.ExpenseJpaRepository;
import com.proiect.repository.ExpenseVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpensePipelineService {

    private final ExtractionClient extractionClient;
    private final ExpenseJpaRepository repository;
    private final ExpenseVectorRepository vectorRepository;
    private final PipelineValidationService validationService;

    @Transactional
    public Long processRawInput(String rawText) {
        log.info("Starting pipeline for raw text: {}", rawText);

        // extraction (Dumitrita's API)
        ExtractedExpenseDTO extracted = extractionClient.extractData(rawText);
        log.info("Extraction result: {}", extracted);

        // save SQL (M1's PostgreSQL)
        ExpenseEntity entity = new ExpenseEntity(
            extracted.amount(),
            extracted.currency(),
            extracted.category(),
            extracted.transactionDate(),
            rawText
        );
        entity = repository.save(entity);
        log.info("Saved entity to SQL Database: {}", entity.getId());

        // save Dummy Vector (Alexia's Qdrant placeholder)
        float[] dummyVector = new float[10];
        for (int i = 0; i < 10; i++) dummyVector[i] = (float) Math.random();
        vectorRepository.saveVector(entity, dummyVector);
        log.info("Sent dummy vector to Alexia's Vector Repository.");

        // validate Persistence
        validationService.validatePersistence(entity.getId(), dummyVector);
        
        log.info("Pipeline completed successfully for ID: {}", entity.getId());
        return entity.getId();
    }
}
