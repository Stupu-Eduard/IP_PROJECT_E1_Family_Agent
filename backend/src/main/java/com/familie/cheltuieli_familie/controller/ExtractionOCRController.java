package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.service.ExtractionPipelineService;
import com.familie.cheltuieli_familie.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
@RequestMapping("/api/ocr")
public class ExtractionOCRController {

    private static final Path OCR_UPLOAD_DIRECTORY =
            Paths.get("uploads", "ocr").toAbsolutePath().normalize();

    private final ExtractionPipelineService extractionPipelineService;
    private final StorageService storageService;

    public ExtractionOCRController(ExtractionPipelineService extractionPipelineService,
                                   StorageService storageService) {
        this.extractionPipelineService = extractionPipelineService;
        this.storageService = storageService;
    }

    @PostMapping("/extract-and-save")
    public ResponseEntity<List<Transaction>> extractAndSave(
            @RequestParam("file") MultipartFile multipartFile,
            @RequestParam(value = "bank", required = false, defaultValue = "unknown") String bank
    ) throws IOException {
        Path tempFilePath = createTemporaryUploadFile(multipartFile);

        try {
            File tempFile = tempFilePath.toFile();

            List<Transaction> transactions = extractionPipelineService.processDocument(tempFile);
            storageService.save(transactions);

            return ResponseEntity.ok(transactions);
        } finally {
            Files.deleteIfExists(tempFilePath);
        }
    }

    private Path createTemporaryUploadFile(MultipartFile multipartFile) throws IOException {
        Files.createDirectories(OCR_UPLOAD_DIRECTORY);

        Path tempFilePath = Files.createTempFile(
                OCR_UPLOAD_DIRECTORY,
                "ocr-upload-",
                ".pdf"
        );

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFilePath;
    }
}
