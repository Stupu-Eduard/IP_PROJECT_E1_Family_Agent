package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.security.util.SecurityService;
import com.familie.cheltuieli_familie.service.ExtractionPipelineService;
import com.familie.cheltuieli_familie.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.familie.cheltuieli_familie.service.CloudinaryService;
import org.springframework.security.core.Authentication;
import com.familie.cheltuieli_familie.model.User;

@RestController
@RequestMapping("/api/ocr")
@PreAuthorize("isAuthenticated()")
public class ExtractionOCRController {

    private static final Path OCR_UPLOAD_DIRECTORY =
            Paths.get("uploads", "ocr").toAbsolutePath().normalize();

    private final ExtractionPipelineService extractionPipelineService;
    private final StorageService storageService;
    private final CloudinaryService cloudinaryService;
    private final SecurityService securityService;

    public ExtractionOCRController(ExtractionPipelineService extractionPipelineService,
                                   StorageService storageService,
                                   CloudinaryService cloudinaryService,
                                   SecurityService securityService) {
        this.extractionPipelineService = extractionPipelineService;
        this.storageService = storageService;
        this.cloudinaryService = cloudinaryService;
        this.securityService = securityService;
    }

    @PostMapping("/extract-and-save")
    public ResponseEntity<List<Transaction>> extractAndSave(
            @RequestParam("file") MultipartFile multipartFile,
            @RequestParam(value = "bank", required = false, defaultValue = "unknown") String bank,
            Authentication authentication
    ) throws IOException {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        Path tempFilePath = createTemporaryUploadFile(multipartFile);

        try {
            File tempFile = tempFilePath.toFile();
            
            String cloudinaryUrl = null;
            String folder = "receipts/" + java.time.LocalDate.now().toString().substring(0, 7);
            String publicId = "ocr_" + currentUser.getId() + "_" + System.currentTimeMillis();
            cloudinaryUrl = cloudinaryService.uploadFile(tempFile, folder, publicId);

            List<Transaction> transactions = extractionPipelineService.processDocument(tempFile, bank);
            
            // Enforce ownership on all extracted transactions
            for (Transaction t : transactions) {
                t.setUserId(currentUser.getId());
                Long familyId = currentUser.getFamily() != null ? currentUser.getFamily().getId() : null;
                t.setFamilyId(familyId);
                if (cloudinaryUrl != null) {
                    t.setReceiptUrl(cloudinaryUrl);
                }
            }
            
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
