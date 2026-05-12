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

    public ExtractionPipelineService(TextBasedPdfExtractor textBasedPdfExtractor,
                                     BankStatementParser bankStatementParser,
                                     BankOcrService bankOcrService) {
        this.textBasedPdfExtractor = textBasedPdfExtractor;
        this.bankStatementParser = bankStatementParser;
        this.bankOcrService = bankOcrService;
    }

    public List<Transaction> processDocument(File file, String bank) throws Exception {
        String rawText;

        if (textBasedPdfExtractor.isTextBased(file)) {
            rawText = textBasedPdfExtractor.extractText(file);
        } else {
            rawText = bankOcrService.extractText(file, bank);
        }

        return bankStatementParser.parseText(rawText);
    }
}