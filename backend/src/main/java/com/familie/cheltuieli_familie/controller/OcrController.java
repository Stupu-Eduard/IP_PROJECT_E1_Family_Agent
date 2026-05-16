package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.OcrResponseDTO;
import com.familie.cheltuieli_familie.event.ExpenseSyncEvent;
import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.model.ExpenseItem;
import com.familie.cheltuieli_familie.model.Location;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.repository.ExpenseItemRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.LocationRepository;
import com.familie.cheltuieli_familie.service.CloudinaryService;
import com.familie.cheltuieli_familie.service.OcrService;
import com.familie.cheltuieli_familie.service.ReceiptParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/ocr")
@Slf4j
public class OcrController {

    private static final Path OCR_UPLOAD_DIRECTORY =
            Paths.get("uploads", "ocr").toAbsolutePath().normalize();

    private final OcrService ocrService;
    private final ReceiptParser receiptParser;
    private final CloudinaryService cloudinaryService;
    private final ExpenseRepository expenseRepository;
    private final ExpenseItemRepository expenseItemRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OcrController(OcrService ocrService,
                         ReceiptParser receiptParser,
                         CloudinaryService cloudinaryService,
                         ExpenseRepository expenseRepository,
                         ExpenseItemRepository expenseItemRepository,
                         CategoryRepository categoryRepository,
                         LocationRepository locationRepository,
                         FamilyMemberRepository familyMemberRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.ocrService = ocrService;
        this.receiptParser = receiptParser;
        this.cloudinaryService = cloudinaryService;
        this.expenseRepository = expenseRepository;
        this.expenseItemRepository = expenseItemRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/process")
    public ResponseEntity<OcrResponseDTO> processReceipt(
            @RequestParam("file") MultipartFile multipartFile,
            Authentication authentication
    ) throws IOException {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Autentificare necesară.");
        }

        String originalName = multipartFile.getOriginalFilename();
        String extension = getExtension(originalName);

        Files.createDirectories(OCR_UPLOAD_DIRECTORY);
        Path tempFilePath = Files.createTempFile(OCR_UPLOAD_DIRECTORY, "ocr-upload-", "." + extension);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            File file = tempFilePath.toFile();
            String ocrText;

            if (isImageFile(extension)) {
                log.info("Processing image file: {} ({} bytes)", originalName, file.length());
                ocrText = ocrService.extractTextFromImage(file);
            } else {
                log.info("Processing PDF file: {} ({} bytes)", originalName, file.length());
                ocrText = ocrService.extractTextFromPdf(file);
            }

            log.debug("OCR text extracted ({} chars): {}", ocrText.length(),
                    ocrText.substring(0, Math.min(200, ocrText.length())));

            // Use LLM-based receipt parser
            ReceiptParser.ParsedReceipt receipt = receiptParser.parseReceipt(ocrText);

            if (receipt == null) {
                log.warn("Receipt parsing failed for file: {}", originalName);
                return ResponseEntity.ok(new OcrResponseDTO(null, null, null, null, 0.0));
            }

            // Lookup or create category
            Category category = categoryRepository.findByName(receipt.category)
                    .orElseGet(() -> {
                        log.warn("Category '{}' not found, defaulting to first available", receipt.category);
                        return categoryRepository.findAll().stream().findFirst().orElse(null);
                    });

            // Lookup or create location
            Location location = null;
            if (receipt.storeName != null && !receipt.storeName.isBlank()) {
                location = locationRepository.findAll().stream()
                        .filter(l -> l.getStore() != null &&
                                l.getStore().equalsIgnoreCase(receipt.storeName))
                        .findFirst()
                        .orElseGet(() -> {
                            Location newLoc = new Location();
                            newLoc.setStore(receipt.storeName.trim());
                            return locationRepository.save(newLoc);
                        });
            }

            // Upload receipt to Cloudinary
            String cloudinaryUrl = null;
            try {
                String folder = "receipts/" + LocalDate.now().toString().substring(0, 7);
                String publicId = "receipt_" + user.getId() + "_" + System.currentTimeMillis();
                cloudinaryUrl = cloudinaryService.uploadFile(file, folder, publicId);
            } catch (Exception e) {
                log.warn("Cloudinary upload failed, continuing without receipt URL: {}", e.getMessage());
            }

            // Build and save expense
            Expense expense = new Expense();
            expense.setAmount(receipt.totalAmount);
            expense.setDescription(receipt.storeName != null ? "Cheltuială OCR: " + receipt.storeName : "Cheltuială OCR");
            LocalDateTime expenseDate;
            if (receipt.date != null && !receipt.date.isBlank()) {
                try {
                    expenseDate = LocalDate.parse(receipt.date).atStartOfDay();
                } catch (Exception e) {
                    log.warn("Failed to parse receipt date '{}', using current date", receipt.date);
                    expenseDate = LocalDateTime.now();
                }
            } else {
                expenseDate = LocalDateTime.now();
            }
            expense.setExpenseDate(expenseDate);
            expense.setCategory(category);
            expense.setLocation(location);
            expense.setUser(user);
            expense.setCurrency("RON");
            expense.setSourceType("OCR");
            expense.setReceiptUrl(cloudinaryUrl);

            familyMemberRepository.findByUserId(user.getId()).stream()
                    .findFirst()
                    .ifPresent(fm -> expense.setFamily(fm.getFamily()));

            // Store the full OCR text for RAG searchability
            expense.setRawInput(ocrText);

            Expense saved = expenseRepository.save(expense);
            log.info("Saved OCR expense id={} amount={} category={} user={}",
                    saved.getId(), saved.getAmount(),
                    category != null ? category.getName() : null,
                    user.getName());

            // Save parsed receipt items for detailed product queries
            if (receipt.items != null && !receipt.items.isEmpty()) {
                for (ReceiptParser.ReceiptItem item : receipt.items) {
                    if (item.name != null && !item.name.isBlank()) {
                        ExpenseItem expenseItem = new ExpenseItem();
                        expenseItem.setExpense(saved);
                        expenseItem.setItemName(item.name.trim());
                        expenseItem.setDescription(item.name.trim());
                        expenseItem.setQuantity(item.quantity != null ? item.quantity : BigDecimal.ONE);
                        expenseItem.setAmount(item.unitPrice != null ? item.unitPrice : BigDecimal.ZERO);
                        expenseItem.setCategory(category);
                        expenseItemRepository.save(expenseItem);
                    }
                }
                log.info("Saved {} items for expense id={}", receipt.items.size(), saved.getId());
            }

            // Sync to Qdrant vector store for semantic / RAG search
            ExpenseEntity entity = toExpenseEntity(saved, ocrText);
            eventPublisher.publishEvent(new ExpenseSyncEvent(this, entity));

            OcrResponseDTO response = new OcrResponseDTO(
                    saved.getAmount(),
                    category != null ? category.getName() : null,
                    receipt.date,
                    receipt.storeName,
                    0.90
            );

            return ResponseEntity.ok(response);

        } finally {
            Files.deleteIfExists(tempFilePath);
        }
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

    private ExpenseEntity toExpenseEntity(Expense expense, String ocrText) {
        ExpenseEntity entity = new ExpenseEntity();
        entity.setId(expense.getId());
        entity.setAmount(expense.getAmount());
        entity.setCategory(expense.getCategory() != null ? expense.getCategory().getName() : null);
        entity.setLocation(expense.getLocation() != null ? expense.getLocation().getStore() : null);
        entity.setPerson(expense.getUser() != null ? expense.getUser().getName() : null);
        entity.setDate(expense.getExpenseDate() != null ? expense.getExpenseDate().toLocalDate() : null);
        // Include full OCR text for semantic search, plus structured summary
        String structured = String.format("Cheltuială OCR: %s, Sumă: %s, Categorie: %s, Magazin: %s",
                expense.getDescription(), expense.getAmount(),
                expense.getCategory() != null ? expense.getCategory().getName() : null,
                expense.getLocation() != null ? expense.getLocation().getStore() : null);
        if (ocrText != null && !ocrText.isBlank()) {
            entity.setRawInput(structured + "\nText extras: " + ocrText.substring(0, Math.min(ocrText.length(), 2000)));
        } else {
            entity.setRawInput(structured);
        }
        entity.setCreatedAt(expense.getCreatedAt() != null ? expense.getCreatedAt() : LocalDateTime.now());
        return entity;
    }
}
