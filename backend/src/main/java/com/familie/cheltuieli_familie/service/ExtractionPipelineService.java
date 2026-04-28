package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.List;

@Service
public class ExtractionPipelineService {

    private final TextBasedPdfExtractor textExtractor;
    private final BankOcrService ocrProcessor;
    private final BankStatementParser bankParser;

    public ExtractionPipelineService(TextBasedPdfExtractor textExtractor, BankOcrService ocrProcessor, BankStatementParser bankParser) {
        this.textExtractor = textExtractor;
        this.ocrProcessor = ocrProcessor;
        this.bankParser = bankParser;
    }

    private String runOcrPipeline(File file, String bank) throws Exception {
        return ocrProcessor.extractText(file, bank);
    }

    private String runTextPipeline(File file) throws Exception {
        return textExtractor.extractText(file);
    }

    public List<Transaction> processDocument(File file, String bank) throws Exception {
        String rawText;
        if (textExtractor.isTextBased(file)) {
            System.out.println("Document digital detectat.");
            rawText = textExtractor.extractText(file);
        } else {
            System.out.println("Document scanat detectat. Folosesc OCR.");
            rawText = ocrProcessor.extractText(file, bank);
        }

        return bankParser.parseText(rawText);
    }
}