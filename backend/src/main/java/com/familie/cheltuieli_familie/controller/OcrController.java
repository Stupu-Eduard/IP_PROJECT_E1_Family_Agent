package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.OcrItemDTO;
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
import com.familie.cheltuieli_familie.service.OcrService.OcrResult;
import com.familie.cheltuieli_familie.service.ReceiptParser;
import lombok.RequiredArgsConstructor;
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
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ocr")
@Slf4j
@RequiredArgsConstructor
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

    @PostMapping("/process")
    public ResponseEntity<OcrResponseDTO> processReceipt(
            @RequestParam("file") MultipartFile multipartFile,
            Authentication authentication
    ) throws IOException {
        User user = extractUser(authentication);
        String originalName = multipartFile.getOriginalFilename();
        String extension = getExtension(originalName);

        Files.createDirectories(OCR_UPLOAD_DIRECTORY);
        Path tempFilePath = Files.createTempFile(OCR_UPLOAD_DIRECTORY, "ocr-upload-", "." + extension);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            File file = tempFilePath.toFile();
            
            // Upload to Cloudinary FIRST so we have the photo even if OCR fails
            String cloudinaryUrl = uploadReceipt(file, user);
            
            OcrResult ocrResult = extractOcrText(file, extension, originalName);
            String ocrText = ocrResult.text();
            double confidence = ocrResult.confidence();
            ReceiptParser.ParsedReceipt receipt = receiptParser.parseReceipt(ocrText);

            if (receipt == null) {
                log.warn("Receipt parsing failed for file: {}. Image stored at: {}", originalName, cloudinaryUrl);
                return ResponseEntity.ok(new OcrResponseDTO(null, null, null, null, confidence, cloudinaryUrl, Collections.emptyList()));
            }

            Category category = resolveCategory(receipt.getCategory());

            log.info("OCR parsed: amount={} category={} store={} date={} items={} url={}",
                    receipt.getTotalAmount(),
                    category != null ? category.getName() : null,
                    receipt.getStoreName(),
                    receipt.getDate(),
                    receipt.getItems().size(),
                    cloudinaryUrl);

            return ResponseEntity.ok(new OcrResponseDTO(
                    receipt.getTotalAmount(),
                    category != null ? category.getName() : null,
                    receipt.getDate(),
                    receipt.getStoreName(),
                    confidence,
                    cloudinaryUrl,
                    mapItems(receipt.getItems())
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

    private OcrResult extractOcrText(File file, String extension, String originalName) {
        if (isImageFile(extension)) {
            log.info("Processing image file: {} ({} bytes)", originalName, file.length());
            return ocrService.extractTextFromImage(file);
        }
        log.info("Processing PDF file: {} ({} bytes)", originalName, file.length());
        String text = ocrService.extractTextFromPdf(file);
        return new OcrResult(text, 0.0);
    }

    private Category resolveCategory(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseGet(() -> {
                    log.warn("Category '{}' not found, defaulting to first available", categoryName);
                    return categoryRepository.findAll().stream().findFirst().orElse(null);
                });
    }

    private Location resolveLocation(String storeName) {
        if (storeName == null || storeName.isBlank()) {
            return null;
        }
        return locationRepository.findAll().stream()
                .filter(l -> l.getStore() != null && l.getStore().equalsIgnoreCase(storeName))
                .findFirst()
                .orElseGet(() -> {
                    Location newLoc = new Location();
                    newLoc.setStore(storeName.trim());
                    return locationRepository.save(newLoc);
                });
    }

    private String uploadReceipt(File file, User user) {
        try {
            String folder = "receipts/" + LocalDate.now().toString().substring(0, 7);
            String publicId = "receipt_" + user.getId() + "_" + System.currentTimeMillis();
            return cloudinaryService.uploadFile(file, folder, publicId);
        } catch (Exception e) {
            log.warn("Cloudinary upload failed, continuing without receipt URL: {}", e.getMessage());
            return null;
        }
    }

    private Expense saveExpense(ReceiptParser.ParsedReceipt receipt, Category category,
                                Location location, User user, String cloudinaryUrl, String ocrText) {
        Expense expense = new Expense();
        expense.setAmount(receipt.getTotalAmount());
        expense.setDescription(receipt.getStoreName() != null ? "Cheltuială OCR: " + receipt.getStoreName() : "Cheltuială OCR");
        expense.setExpenseDate(parseExpenseDate(receipt.getDate()));
        expense.setCategory(category);
        expense.setLocation(location);
        expense.setUser(user);
        expense.setCurrency("RON");
        expense.setSourceType("OCR");
        expense.setReceiptUrl(cloudinaryUrl);
        expense.setRawInput(ocrText);

        familyMemberRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .ifPresent(fm -> expense.setFamily(fm.getFamily()));

        Expense saved = expenseRepository.save(expense);
        log.info("Saved OCR expense id={} amount={} category={} user={}",
                saved.getId(), saved.getAmount(),
                category != null ? category.getName() : null,
                user.getName());
        return saved;
    }

    private LocalDateTime parseExpenseDate(String dateStr) {
        if (dateStr != null && !dateStr.isBlank()) {
            try {
                return LocalDate.parse(dateStr).atStartOfDay();
            } catch (Exception e) {
                log.warn("Failed to parse receipt date '{}', using current date", dateStr);
            }
        }
        return LocalDateTime.now();
    }

    private void saveExpenseItems(Expense expense, List<ReceiptParser.ReceiptItem> items, Category category) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (ReceiptParser.ReceiptItem item : items) {
            if (item.getName() != null && !item.getName().isBlank()) {
                ExpenseItem expenseItem = new ExpenseItem();
                expenseItem.setExpense(expense);
                expenseItem.setItemName(item.getName().trim());
                expenseItem.setDescription(item.getName().trim());
                expenseItem.setQuantity(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE);
                expenseItem.setAmount(item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO);
                expenseItem.setCategory(category);
                expenseItemRepository.save(expenseItem);
            }
        }
        log.info("Saved {} items for expense id={}", items.size(), expense.getId());
    }

    private void publishSyncEvent(Expense expense, String ocrText) {
        ExpenseEntity entity = toExpenseEntity(expense, ocrText);
        eventPublisher.publishEvent(new ExpenseSyncEvent(this, entity));
    }

    private OcrResponseDTO buildResponse(Expense saved, ReceiptParser.ParsedReceipt receipt, Category category, double confidence) {
        return new OcrResponseDTO(
                saved.getAmount(),
                category != null ? category.getName() : null,
                receipt.getDate(),
                receipt.getStoreName(),
                confidence,
                saved.getReceiptUrl(),
                mapItems(receipt.getItems())
        );
    }

    private List<OcrItemDTO> mapItems(List<ReceiptParser.ReceiptItem> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        return items.stream()
                .filter(i -> i.getName() != null && !i.getName().isBlank())
                .map(i -> new OcrItemDTO(i.getName(), i.getQuantity(), i.getUnitPrice()))
                .toList();
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
