package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class ExtractionPipelineService {

    private final TextBasedPdfExtractor textBasedPdfExtractor;
    private final BankStatementParser bankStatementParser;
    private final BankOcrService bankOcrService;
    private final OcrService ocrService;

    public ExtractionPipelineService(TextBasedPdfExtractor textBasedPdfExtractor,
                                     BankStatementParser bankStatementParser,
                                     BankOcrService bankOcrService,
                                     OcrService ocrService) {
        this.textBasedPdfExtractor = textBasedPdfExtractor;
        this.bankStatementParser = bankStatementParser;
        this.bankOcrService = bankOcrService;
        this.ocrService = ocrService;
    }

    public List<Transaction> processDocument(File file) throws java.io.IOException {
        // First try text-based extraction
        if (textBasedPdfExtractor.isTextBased(file)) {
            String rawText = textBasedPdfExtractor.extractText(file);
            return bankStatementParser.parseText(rawText);
        }

        // Fallback to OCR for image-based PDFs
        try {
            String ocrText = ocrService.extractTextFromPdf(file);
            return bankStatementParser.parseText(ocrText);
        } catch (Exception e) {
            throw new java.io.IOException("Failed to extract text from PDF using both text and OCR methods", e);
        }
    }

    public List<Transaction> processDocument(File file, String bank) throws java.io.IOException {
        // First try text-based extraction
        if (textBasedPdfExtractor.isTextBased(file)) {
            String rawText = textBasedPdfExtractor.extractText(file);
            return bankStatementParser.parseText(rawText);
        }

        // Fallback to bank-specific OCR pipeline
        try {
            String ocrText = bankOcrService.extractText(file, bank);
            return bankStatementParser.parseText(ocrText);
        } catch (Exception e) {
            // Final fallback to generic OCR
            try {
                String ocrText = ocrService.extractTextFromPdf(file);
                return bankStatementParser.parseText(ocrText);
            } catch (Exception e2) {
                throw new java.io.IOException("Failed to extract text from PDF using all available methods", e2);
            }
        }
    }
}