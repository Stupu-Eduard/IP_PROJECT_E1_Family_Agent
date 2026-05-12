package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.ExtractionRequest;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.exception.AiServiceException;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.service.ExtractionService;
import com.familie.cheltuieli_familie.service.OcrService;
import com.familie.cheltuieli_familie.service.PdfExtractionService;
import com.familie.cheltuieli_familie.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/v1/upload")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FileUploadController {

    private final PdfExtractionService pdfExtractionService;
    private final ExtractionService extractionService;
    private final SyncService syncService;
    private final OcrService ocrService;

    @PostMapping("/pdf")
    public ResponseEntity<List<ExtractionResponse>> uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("Received PDF upload: {}", file.getOriginalFilename());
        String extractedText;
        try {
            extractedText = pdfExtractionService.extractText(file);
            if (extractedText == null || extractedText.isBlank()) {
                throw new AiServiceException("PDF extraction returned empty text");
            }
        } catch (AiServiceException e) {
            log.warn("PDF text extraction failed or returned empty, falling back to OCR: {}", e.getMessage());
            File tempFile = createSecureTempFile("upload-", ".pdf");
            file.transferTo(tempFile);
            try {
                extractedText = ocrService.extractTextFromPdf(tempFile);
            } finally {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException ex) {
                    log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath(), ex);
                }
            }
        }

        ExtractionRequest request = new ExtractionRequest();
        request.setRawText(extractedText);
        List<ExtractionResponse> responses = extractionService.process(request);

        for (ExtractionResponse response : responses) {
            Expense expense = Expense.builder()
                    .amount(response.getAmount())
                    .aiCategory(response.getCategory())
                    .aiLocation(response.getLocation())
                    .aiPerson(response.getPerson())
                    .expenseDate(response.getTransactionDate().atStartOfDay())
                    .rawInput(response.getRawInput())
                    .build();
            syncService.syncExpense(expense);
        }

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/audio")
    public ResponseEntity<String> uploadAudio(@RequestParam("file") MultipartFile file) {
        log.warn("Audio transcription is not available in this version. Filename: {}", file.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("Audio transcription is not available. Please use PDF upload instead.");
    }

    @PostMapping("/image")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        log.warn("OCR upload attempted by M5 team is not yet integrated. Filename: {}", file.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("OCR module is under development by the M5 team. Please use PDF upload instead.");
    }

    private File createSecureTempFile(String prefix, String suffix) throws IOException {
        Path tempDir = Paths.get(System.getProperty("user.dir"), "secure-temp");
        Files.createDirectories(tempDir);
        Path secureFile = Files.createTempFile(tempDir, prefix, suffix);
        return secureFile.toFile();
    }
}
