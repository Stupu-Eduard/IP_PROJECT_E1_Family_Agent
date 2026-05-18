package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.OcrResponseDTO;
import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.service.OcrService;
import com.familie.cheltuieli_familie.service.ReceiptParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/api/v1/ocr")
@Slf4j
@RequiredArgsConstructor
public class OcrController {

    private static final Path OCR_UPLOAD_DIRECTORY =
            Paths.get("uploads", "ocr").toAbsolutePath().normalize();

    private final OcrService ocrService;
    private final ReceiptParser receiptParser;
    private final CategoryRepository categoryRepository;

    @PostMapping("/process")
    public ResponseEntity<OcrResponseDTO> processReceipt(
            @RequestParam("file") MultipartFile multipartFile,
            Authentication authentication
    ) throws IOException {
        extractUser(authentication);
        String originalName = multipartFile.getOriginalFilename();
        String extension = getExtension(originalName);

        Files.createDirectories(OCR_UPLOAD_DIRECTORY);
        Path tempFilePath = Files.createTempFile(OCR_UPLOAD_DIRECTORY, "ocr-upload-", "." + extension);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            File file = tempFilePath.toFile();
            String ocrText = extractOcrText(file, extension, originalName);
            ReceiptParser.ParsedReceipt receipt = receiptParser.parseReceipt(ocrText);

            if (receipt == null) {
                log.warn("Receipt parsing failed for file: {}", originalName);
                return ResponseEntity.ok(new OcrResponseDTO(null, null, null, null, 0.0));
            }

            Category category = resolveCategory(receipt.getCategory());
            log.info("OCR parsed: amount={} category={} store={} date={}",
                    receipt.getTotalAmount(),
                    category != null ? category.getName() : null,
                    receipt.getStoreName(),
                    receipt.getDate());

            return ResponseEntity.ok(new OcrResponseDTO(
                    receipt.getTotalAmount(),
                    category != null ? category.getName() : null,
                    receipt.getDate(),
                    receipt.getStoreName(),
                    0.90
            ));

        } finally {
            Files.deleteIfExists(tempFilePath);
        }
    }

    private User extractUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Autentificare necesară.");
        }
        return user;
    }

    private String extractOcrText(File file, String extension, String originalName) {
        if (isImageFile(extension)) {
            log.info("Processing image file: {} ({} bytes)", originalName, file.length());
            return ocrService.extractTextFromImage(file);
        }
        log.info("Processing PDF file: {} ({} bytes)", originalName, file.length());
        return ocrService.extractTextFromPdf(file);
    }

    private Category resolveCategory(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseGet(() -> {
                    log.warn("Category '{}' not found, defaulting to first available", categoryName);
                    return categoryRepository.findAll().stream().findFirst().orElse(null);
                });
    }

    private boolean isImageFile(String extension) {
        if (extension == null) return false;
        return extension.equalsIgnoreCase("jpg")
                || extension.equalsIgnoreCase("jpeg")
                || extension.equalsIgnoreCase("png")
                || extension.equalsIgnoreCase("webp")
                || extension.equalsIgnoreCase("bmp");
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "tmp";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
