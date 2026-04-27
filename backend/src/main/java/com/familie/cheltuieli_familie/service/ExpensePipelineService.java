package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.service.ExtractionService;
import com.familie.cheltuieli_familie.dto.ExtractionRequest;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpensePipelineService {

    private final ExtractionService extractionService;
    private final SyncService syncService;
    private final PipelineValidationService validationService;

    @Transactional
    public List<Long> processRawInput(String rawText) {
        log.info("Starting pipeline for raw text: {}", rawText);

        // extraction (OpenAI/DeepSeek)
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText(rawText);
        List<ExtractionResponse> extractedList = extractionService.process(req);
        log.info("Extraction result: {} entities found", extractedList.size());

        return extractedList.stream().map(extracted -> {
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
            
            return entity.getId();
        }).collect(Collectors.toList());
    }
}
