package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.exception.PipelineException;
import com.familie.cheltuieli_familie.dto.ExtractionRequest;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.model.Expense;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpensePipelineService {

    private final ExtractionService extractionService;
    private final SyncService syncService;
    private final PipelineValidationService validationService;
    private final ThePipeHandler thePipeHandler;
    private final ObjectMapper objectMapper;

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
            Expense expense = Expense.builder()
                    .amount(extracted.getAmount())
                    .aiCategory(extracted.getCategory())
                    .aiLocation(extracted.getLocation())
                    .aiPerson(extracted.getPerson())
                    .expenseDate(extracted.getTransactionDate().atStartOfDay())
                    .rawInput(extracted.getRawInput())
                    .build();
            
            expense = syncService.syncExpense(expense);
            log.info("Saved and synced expense: {}", expense.getId());

            // validate Persistence
            validationService.validatePersistence(expense.getId());

            // --- THE PIPE: Trimitem notificarea în timp real ---
            try {
                String payload = objectMapper.writeValueAsString(expense);
                thePipeHandler.broadcast(payload);
            } catch (Exception e) {
                log.error("Failed to broadcast expense to The Pipe", e);
            }
            // --------------------------------------------------
            
            return expense.getId();
        }).toList();
    }
}
