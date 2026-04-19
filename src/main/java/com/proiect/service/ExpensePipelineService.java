package com.proiect.service;

import com.proiect.service.ExtractionService;
import com.proiect.dto.ExtractionRequest;
import com.proiect.dto.ExtractionResponse;
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

    private final ExtractionService extractionService;
    private final ExpenseJpaRepository repository;
    private final ExpenseVectorRepository vectorRepository;
    private final PipelineValidationService validationService;

    @Transactional
    public Long processRawInput(String rawText) {
        log.info("Starting pipeline for raw text: {}", rawText);

        // extraction (Dumitrita's API locally)
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText(rawText);
        ExtractionResponse extracted = extractionService.process(req);
        log.info("Extraction result: {}", extracted);

        // save SQL (M1's PostgreSQL)
        ExpenseEntity entity = ExpenseEntity.builder()
                .amount(extracted.getAmount())
                .category(extracted.getCategory())
                .location(extracted.getLocation())
                .person(extracted.getPerson())
                .date(extracted.getTransactionDate())
                .rawInput(extracted.getRawInput())
                .build();
        
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
