package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.ExtractionRequest;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.service.ExtractionService;
import com.familie.cheltuieli_familie.service.SyncService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/extract")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExtractionController {

    private final ExtractionService extractionService;
    private final SyncService syncService;

    @PostMapping
    public ResponseEntity<List<ExtractionResponse>> extractDetails(@Valid @RequestBody ExtractionRequest request) {
        List<ExtractionResponse> responses = extractionService.process(request);

        for (ExtractionResponse response : responses) {
            ExpenseEntity entity = ExpenseEntity.builder()
                    .amount(response.getAmount())
                    .category(response.getCategory())
                    .location(response.getLocation())
                    .person(response.getPerson())
                    .date(response.getTransactionDate())
                    .rawInput(response.getRawInput())
                    .build();
            syncService.syncExpense(entity);
        }

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/validate-ocr")
    public ResponseEntity<String> validateOcr(@RequestBody String rawOcrText) {
        String validationResult = extractionService.validateOcrContent(rawOcrText);
        return ResponseEntity.ok(validationResult);
    }
}
