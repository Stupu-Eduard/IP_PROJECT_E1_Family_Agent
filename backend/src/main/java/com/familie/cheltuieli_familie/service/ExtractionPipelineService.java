package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class ExtractionPipelineService {

    private final TextBasedPdfExtractor textBasedPdfExtractor;
    private final BankStatementParser bankStatementParser;

    public ExtractionPipelineService(TextBasedPdfExtractor textBasedPdfExtractor,
                                     BankStatementParser bankStatementParser) {
        this.textBasedPdfExtractor = textBasedPdfExtractor;
        this.bankStatementParser = bankStatementParser;
    }

    public List<Transaction> processDocument(File file) throws Exception {
        String rawText = textBasedPdfExtractor.extractText(file);
        return bankStatementParser.parseText(rawText);
    }
}