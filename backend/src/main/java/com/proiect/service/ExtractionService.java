package com.proiect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proiect.exception.AmountNotFoundException;
import com.proiect.model.ExpenseEntityDumitrita;
import com.proiect.dto.ExtractionRequest;
import com.proiect.dto.ExtractionResponse;
import com.proiect.repository.ExpenseRepositoryDumitrita;
import com.proiect.util.NormalizerUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractionService {

    private final ChatLanguageModel chatLanguageModel;
    private final ExpenseRepositoryDumitrita expenseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    interface ExtractionAssistant {
        @UserMessage("""
            {
              "role": "Financial Receipt Expert Analyst",
              "goal": "Extract deterministic financial data from raw OCR Romanian text",
              "inputs": {"raw_text": "{{rawText}}"},
              "style": { "audience": "system_DB", "tone": "stoic_deterministic" },
              "rules": ["No conversational text", "Infer location if obvious", "Default to RON"],
              "steps": ["Scan for monetary amounts", "Identify category", "Extract dates"],
              "output": {
                "format": "json",
                "schema_note": "Strict JSON. Types matter.",
                "example_shape": {
                  "amount": "decimal",
                  "category": "string",
                  "location": "string",
                  "person": "string",
                  "transactionDate": "YYYY-MM-DD"
                }
              },
              "params": {"temperature": 0.1, "max_tokens": 500}
            }
            """)
        String extract(@dev.langchain4j.service.V("rawText") String rawText);

        @SystemMessage("Ești un expert în validare OCR. Verifică dacă suma prețurilor articolelor individuale dintr-un bon coincide cu totalul extras. " +
                "Răspunde doar cu 'VALID' sau 'INVALID: [motiv]'.")
        String validateOcr(@UserMessage String rawOcrText);
    }

    /**
     * Strips markdown code fences (```json ... ```) that some LLMs wrap around JSON output.
     */
    private String stripMarkdownFences(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        // Remove leading ```json or ``` then trailing ```
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("```json\\s*", "").replaceFirst("```\\s*", "");
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence >= 0) trimmed = trimmed.substring(0, lastFence).trim();
        }
        return trimmed;
    }

    @Transactional
    public ExtractionResponse process(ExtractionRequest request) {
        ExtractionAssistant assistant = AiServices.create(ExtractionAssistant.class, chatLanguageModel);
        
        String rawResult = assistant.extract(request.getRawText());
        log.info("AI Raw Extraction: {}", rawResult);
        String jsonResult = stripMarkdownFences(rawResult);
        log.info("AI Cleaned JSON: {}", jsonResult);

        try {
            JsonNode root = objectMapper.readTree(jsonResult);
            
            // Extract amount with normalization
            String amountStr = root.path("amount").asText();
            BigDecimal amount = NormalizerUtil.normalizeAmount(amountStr);
            
            if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
                // Try normalizing from the original text if AI failed to give a clean number
                amount = NormalizerUtil.normalizeAmount(request.getRawText());
            }

            if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
                throw new AmountNotFoundException("Nu s-a putut identifica suma tranzacției în textul: " + request.getRawText());
            }

            LocalDateTime transactionDate = NormalizerUtil.normalizeDate(request.getRawText());
            String currency = root.path("currency").asText("RON");
            String category = root.path("category").asText("Altele");
            String location = root.path("location").asText("Necunoscut");
            String person = root.path("person").asText("Familie");

            ExpenseEntityDumitrita entity = ExpenseEntityDumitrita.builder()
                    .amount(amount)
                    .category(category)
                    .location(location)
                    .person(person)
                    .transactionDate(transactionDate)
                    .rawInput(request.getRawText())
                    .build();

            expenseRepository.save(entity);

            return ExtractionResponse.builder()
                    .amount(amount)
                    .category(category)
                    .location(location)
                    .person(person)
                    .transactionDate(transactionDate)
                    .rawInput(request.getRawText())
                    .build();

        } catch (Exception e) {
            log.error("Error processing extraction", e);
            if (e instanceof AmountNotFoundException) throw (AmountNotFoundException) e;
            throw new RuntimeException("Eroare internă la procesarea AI", e);
        }
    }

    public String validateOcrContent(String rawOcrText) {
        ExtractionAssistant assistant = AiServices.create(ExtractionAssistant.class, chatLanguageModel);
        return assistant.validateOcr(rawOcrText);
    }
}
