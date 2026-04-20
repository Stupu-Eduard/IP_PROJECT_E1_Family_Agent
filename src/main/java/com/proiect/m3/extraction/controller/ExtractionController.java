package com.proiect.m3.extraction.controller;

import com.proiect.m3.extraction.model.ExtractionRequest;
import com.proiect.m3.extraction.model.ExtractionResponse;
import com.proiect.m3.extraction.service.ExtractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/extract")
public class ExtractionController {

    private final ExtractionService extractionService;

    public ExtractionController(ExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @PostMapping
    public ResponseEntity<ExtractionResponse> extractDetails(@RequestBody ExtractionRequest request) {
        ExtractionResponse response = extractionService.process(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-ocr")
    public ResponseEntity<String> validateOcr(@RequestBody String rawOcrText) {
        String validationResult = extractionService.validateOcrContent(rawOcrText);
        return ResponseEntity.ok(validationResult);
    }
}
