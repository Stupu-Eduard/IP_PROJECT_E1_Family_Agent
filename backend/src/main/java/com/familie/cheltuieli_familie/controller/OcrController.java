package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.ExtractionRequest;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.service.ExtractionService;
import com.familie.cheltuieli_familie.service.OcrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ocr")
@CrossOrigin(origins = {"https://family-agent.me", "http://localhost:5173"})
public class OcrController {

    private final OcrService ocrService;
    private final ExtractionService extractionService;

    public OcrController(OcrService ocrService, ExtractionService extractionService) {
        this.ocrService = ocrService;
        this.extractionService = extractionService;
    }

    @PostMapping("/process")
    public ResponseEntity<List<ExtractionResponse>> processReceipt(@RequestParam("file") MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("ocr_", "_" + file.getOriginalFilename());
        file.transferTo(tempFile);

        String text = ocrService.extractTextFromImage(tempFile);

        ExtractionRequest request = new ExtractionRequest();
        request.setRawText(text);

        List<ExtractionResponse> result = extractionService.process(request);

        tempFile.delete();
        return ResponseEntity.ok(result);
    }
}