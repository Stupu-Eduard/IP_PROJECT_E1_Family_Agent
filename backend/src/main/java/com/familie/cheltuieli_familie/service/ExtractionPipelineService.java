package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExtractionPipelineService {

    private final TextBasedPdfExtractor textBasedPdfExtractor;
    private final BankStatementParser bankStatementParser;
    private final BankOcrService bankOcrService;
    private final OcrService ocrService;
    private final BankStatementLlmParser bankStatementLlmParser;

    public ExtractionPipelineService(TextBasedPdfExtractor textBasedPdfExtractor,
                                     BankStatementParser bankStatementParser,
                                     BankOcrService bankOcrService,
                                     OcrService ocrService,
                                     BankStatementLlmParser bankStatementLlmParser) {
        this.textBasedPdfExtractor = textBasedPdfExtractor;
        this.bankStatementParser = bankStatementParser;
        this.bankOcrService = bankOcrService;
        this.ocrService = ocrService;
        this.bankStatementLlmParser = bankStatementLlmParser;
    }

    public List<Transaction> processDocument(File file) throws java.io.IOException {
        // First try text-based extraction
        if (textBasedPdfExtractor.isTextBased(file)) {
            String rawText = textBasedPdfExtractor.extractText(file);
            List<Transaction> transactions = bankStatementParser.parseText(rawText);
            if (hasGoodCoverage(transactions, rawText)) {
                return transactions;
            }
            return fallbackToLlm(rawText);
        }

        // Fallback to OCR for image-based PDFs
        try {
            String ocrText = ocrService.extractTextFromPdf(file);
            List<Transaction> transactions = bankStatementParser.parseText(ocrText);
            if (hasGoodCoverage(transactions, ocrText)) {
                return transactions;
            }
            return fallbackToLlm(ocrText);
        } catch (Exception e) {
            throw new java.io.IOException("Failed to extract text from PDF using both text and OCR methods", e);
        }
    }

    public List<Transaction> processDocument(File file, String bank) throws java.io.IOException {
        // First try text-based extraction
        if (textBasedPdfExtractor.isTextBased(file)) {
            String rawText = textBasedPdfExtractor.extractText(file);
            List<Transaction> transactions = bankStatementParser.parseText(rawText);
            if (hasGoodCoverage(transactions, rawText)) {
                return transactions;
            }
            return fallbackToLlm(rawText);
        }

        // Fallback to bank-specific OCR pipeline
        try {
            String ocrText = bankOcrService.extractText(file, bank);
            List<Transaction> transactions = bankStatementParser.parseText(ocrText);
            if (hasGoodCoverage(transactions, ocrText)) {
                return transactions;
            }
            return fallbackToLlm(ocrText);
        } catch (Exception e) {
            // Final fallback to generic OCR
            try {
                String ocrText = ocrService.extractTextFromPdf(file);
                List<Transaction> transactions = bankStatementParser.parseText(ocrText);
                if (hasGoodCoverage(transactions, ocrText)) {
                    return transactions;
                }
                return fallbackToLlm(ocrText);
            } catch (Exception e2) {
                throw new java.io.IOException("Failed to extract text from PDF using all available methods", e2);
            }
        }
    }

    private boolean hasGoodCoverage(List<Transaction> transactions, String rawText) {
        // Heuristic: if text is substantial (>500 chars) but we parsed very few transactions,
        // coverage is likely poor — fall back to LLM for better extraction.
        int textLen = rawText != null ? rawText.length() : 0;
        return transactions != null && !transactions.isEmpty() && !(textLen > 500 && transactions.size() < 3);
    }

    private List<Transaction> fallbackToLlm(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            return List.of();
        }
        List<BankStatementLlmParser.ParsedTransaction> parsed = bankStatementLlmParser.parse(ocrText);
        return parsed.stream()
                .map(pt -> {
                    LocalDate date = parseDate(pt.getDate());
                    return new Transaction(date, pt.getDescription(), pt.getAmount(), pt.getCurrency(), pt.getType());
                })
                .toList();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ignored) {
                // Date format did not match, try next formatter.
            }
        }
        return null;
    }
}