package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.ExtractionRequest;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.service.ExtractionPipelineService;
import com.familie.cheltuieli_familie.service.ExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/extract")
@RequiredArgsConstructor
public class ExtractionController {

    private final ExtractionService extractionService;

    private final ExtractionPipelineService orchestrator;

    @PostMapping
    public ResponseEntity<List<ExtractionResponse>> extractDetails(@Valid @RequestBody ExtractionRequest request) {
        List<ExtractionResponse> response = extractionService.process(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-ocr")
    public ResponseEntity<String> validateOcr(@RequestBody String rawOcrText) {
        String validationResult = extractionService.validateOcrContent(rawOcrText);
        return ResponseEntity.ok(validationResult);
    }

    private File createSecureTempFile(String prefix, String suffix) throws IOException {
        Path tempDir = Paths.get(System.getProperty("user.dir"), "secure-temp");
        Files.createDirectories(tempDir);
        Path secureFile = Files.createTempFile(tempDir, prefix, suffix);
        return secureFile.toFile();
    }

    @PostMapping("/process")
    public ResponseEntity<List<Transaction>> processDocument(
            @RequestParam("file") MultipartFile multipartFile,
            @RequestParam("bank") String bank) throws Exception {

        File tempFile = null;

        try {
            tempFile = createSecureTempFile("upload_", ".pdf");
            multipartFile.transferTo(tempFile);

            List<Transaction> transactions = orchestrator.processDocument(tempFile);
            return ResponseEntity.ok(transactions);

        } catch (Exception e) {
            log.error("Eroare la procesarea documentului: {}", e.getMessage(), e);
            throw new IllegalStateException("Eroare la procesarea documentului: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                } catch (IOException e) {
                    log.warn("Nu s-a putut sterge fisierul temporar: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }
}
