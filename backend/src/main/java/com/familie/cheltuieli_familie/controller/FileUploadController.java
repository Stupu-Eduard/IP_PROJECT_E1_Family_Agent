package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.ExtractionRequest;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.service.ExtractionService;
import com.familie.cheltuieli_familie.service.PdfExtractionService;
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

    private final CloudinaryService cloudinaryService;

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
        try {
            log.info("Received image upload for Cloudinary storage: {}", file.getOriginalFilename());
            String folder = "uploads/" + java.time.LocalDate.now().toString().substring(0, 7);
            String publicId = "upload_" + System.currentTimeMillis();
            String url = cloudinaryService.uploadMultipartFile(file, folder, publicId);
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image to Cloudinary: " + e.getMessage());
        }
    }
}
