package com.proiect.controller;

import com.proiect.dto.ExtractionRequest;
import com.proiect.dto.ExtractionResponse;
import com.proiect.service.ExtractionService;
import com.proiect.service.PdfExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;

@RestController
@RequestMapping("/v1/upload")
@Slf4j
@RequiredArgsConstructor
public class FileUploadController {

    private final PdfExtractionService pdfExtractionService;
    private final ExtractionService extractionService;

    @PostMapping("/pdf")
    public ResponseEntity<List<ExtractionResponse>> uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("Received PDF upload: {}", file.getOriginalFilename());
        String extractedText = pdfExtractionService.extractText(file);
        ExtractionRequest request = new ExtractionRequest();
        request.setRawText(extractedText);
        return ResponseEntity.ok(extractionService.process(request));
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
}
