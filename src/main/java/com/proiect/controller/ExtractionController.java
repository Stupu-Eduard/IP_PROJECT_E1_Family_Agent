package com.proiect.controller;

import com.proiect.dto.ExtractionRequest;
import com.proiect.dto.ExtractionResponse;
import com.proiect.service.ExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/extract")
@RequiredArgsConstructor
public class ExtractionController {

    private final ExtractionService extractionService;

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
