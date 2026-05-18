package com.familie.cheltuieli_familie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BankStatementLlmParser {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    public BankStatementLlmParser(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = new ObjectMapper();
    }

    public List<ParsedTransaction> parse(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            log.warn("OCR text is empty, cannot parse bank statement");
            return List.of();
        }

        try {
            String json = extractJson(ocrText);
            return parseTransactions(json);
        } catch (Exception e) {
            log.error("Failed to parse bank statement with LLM: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private String extractJson(String ocrText) {
        BankExtractor extractor = AiServices.builder(BankExtractor.class)
                .chatLanguageModel(chatLanguageModel)
                .build();

        String json = extractor.extract(ocrText);
        log.info("LLM bank statement extraction raw JSON: {}", json);

        return stripMarkdownFences(json);
    }

    private List<ParsedTransaction> parseTransactions(String json) throws Exception {
        ParsedTransaction[] transactions = objectMapper.readValue(json, ParsedTransaction[].class);
        if (transactions == null || transactions.length == 0) {
            log.warn("LLM extraction returned no transactions");
            return List.of();
        }
        return List.of(transactions);
    }

    private String stripMarkdownFences(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("```json\\s*", "").replaceFirst("```\\s*", "");
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence >= 0) trimmed = trimmed.substring(0, lastFence).trim();
        }
        return trimmed;
    }

    public interface BankExtractor {
        @SystemMessage("""
            Ești un parser inteligent de extrase bancare. Extrage toate tranzacțiile structurate din textul OCR furnizat.

            Reguli:
            1. Identifică fiecare tranzacție individuală.
            2. Pentru fiecare tranzacție extrage: data (dd/MM/yyyy), descrierea, suma, moneda (RON/EUR/USD), și tipul (EXPENSE/INCOME/TRANSFER).
            3. Sumele pozitive care apar în coloana de DEBIT sau sunt plăți sunt EXPENSE.
            4. Sumele pozitive care apar în coloana de CREDIT sau sunt încasări sunt INCOME.
            5. Transferurile între conturi proprii sunt TRANSFER.
            6. Ignoră liniile care nu sunt tranzacții (header, footer, totaluri, solduri).
            7. Dacă moneda nu este specificată, folosește RON.

            Răspunde EXCLUSIV cu un JSON valid - un array de obiecte tranzacție, fără markdown, fără explicații suplimentare.
            Format:
            [
              {"date": "dd/MM/yyyy", "description": "string", "amount": 123.45, "currency": "RON", "type": "EXPENSE"},
              ...
            ]
            """)
        String extract(@UserMessage String ocrText);
    }

    public static class ParsedTransaction {
        private String date;
        private String description;
        private double amount;
        private String currency = "RON";
        private String type = "EXPENSE";

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
