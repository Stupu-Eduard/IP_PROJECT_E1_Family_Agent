package com.proiect.m3.extraction.controller;

import com.proiect.m3.extraction.model.ExtractionRequest;
import com.proiect.m3.extraction.model.ExtractionResponse;
import com.proiect.m3.extraction.service.ExtractionService;
import com.proiect.m3.extraction.service.PdfExtractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/v1/upload")
public class FileUploadController {

    private final PdfExtractionService pdfExtractionService;
    private final ExtractionService extractionService;

    public FileUploadController(PdfExtractionService pdfExtractionService, ExtractionService extractionService) {
        this.pdfExtractionService = pdfExtractionService;
        this.extractionService = extractionService;
    }

    @PostMapping("/pdf")
    public ResponseEntity<ExtractionResponse> uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {
        String extractedText = pdfExtractionService.extractText(file);
        ExtractionRequest request = new ExtractionRequest(extractedText);
        return ResponseEntity.ok(extractionService.process(request));
    }
}
