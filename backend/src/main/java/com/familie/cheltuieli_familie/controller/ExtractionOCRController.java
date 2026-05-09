package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.service.ExtractionPipelineService;
import com.familie.cheltuieli_familie.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/ocr")
public class ExtractionOCRController {

    private final ExtractionPipelineService extractionPipelineService;
    private final StorageService storageService;

    public ExtractionOCRController(ExtractionPipelineService extractionPipelineService,
                                StorageService storageService) {
        this.extractionPipelineService = extractionPipelineService;
        this.storageService = storageService;
    }

    @PostMapping("/extract-and-save")
    public ResponseEntity<List<Transaction>> extractAndSave(@RequestParam("file") MultipartFile multipartFile) throws Exception {
        File tempFile = File.createTempFile("ocr-upload-", ".pdf");
        multipartFile.transferTo(tempFile);

        List<Transaction> transactions = extractionPipelineService.processDocument(tempFile);
        storageService.save(transactions);

        tempFile.delete();

        return ResponseEntity.ok(transactions);
    }
}