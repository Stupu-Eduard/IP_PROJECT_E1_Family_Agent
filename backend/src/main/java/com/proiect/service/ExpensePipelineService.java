package com.proiect.service;

import com.proiect.service.ExtractionService;
import com.proiect.dto.ExtractionRequest;
import com.proiect.dto.ExtractionResponse;
import com.proiect.model.ExpenseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpensePipelineService {

    private final ExtractionService extractionService;
    private final SyncService syncService;
    private final PipelineValidationService validationService;

    @Transactional
    public Long processRawInput(String rawText) {
        log.info("Starting pipeline for raw text: {}", rawText);

        // extraction (Dumitrita's API locally)
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText(rawText);
        ExtractionResponse extracted = extractionService.process(req);
        log.info("Extraction result: {}", extracted);

        // save and sync (PostgreSQL + Qdrant)
        ExpenseEntity entity = ExpenseEntity.builder()
                .amount(extracted.getAmount())
                .category(extracted.getCategory())
                .location(extracted.getLocation())
                .person(extracted.getPerson())
                .date(extracted.getTransactionDate())
                .rawInput(extracted.getRawInput())
                .build();
        
        entity = syncService.syncExpense(entity);
        log.info("Saved and synced entity: {}", entity.getId());

        // validate Persistence
        validationService.validatePersistence(entity.getId());
        
        log.info("Pipeline completed successfully for ID: {}", entity.getId());
        return entity.getId();
    }
}
