package com.proiect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proiect.exception.AmountNotFoundException;
import com.proiect.model.ExpenseEntity;
import com.proiect.dto.ExtractionRequest;
import com.proiect.dto.ExtractionResponse;
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
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractionService {

    private final ChatLanguageModel chatLanguageModel;
    private final SyncService syncService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {2000, 4000};

    interface ExtractionAssistant {
        @SystemMessage("""
            Ești un expert în extragerea datelor financiare din texte în limba română.
            Extrage suma, categoria, locația, persoana și data tranzacției din textul furnizat.
            Răspunde DOAR cu un obiect JSON valid, fără text suplimentar, fără explicații, fără markdown.
            Format JSON obligatoriu:
            {
              "amount": "număr zecimal (ex: 150.00)",
              "category": "string",
              "location": "string",
              "person": "string",
              "transactionDate": "YYYY-MM-DD"
            }
            Dacă nu poți extrage un câmp, folosește valori implicite: category="Altele", location="Necunoscut", person="Familie".
            """)
        @UserMessage("Text de procesat: {{rawText}}")
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

    private String callExtractionWithRetry(String rawText) {
        ExtractionAssistant assistant = AiServices.create(ExtractionAssistant.class, chatLanguageModel);
        String lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String rawResult = assistant.extract(rawText);
                log.info("AI Raw Extraction (attempt {}): {}", attempt, rawResult);
                String jsonResult = stripMarkdownFences(rawResult);
                log.info("AI Cleaned JSON (attempt {}): {}", attempt, jsonResult);

                // Validate it's parseable JSON before returning
                objectMapper.readTree(jsonResult);
                return jsonResult;
            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("Extraction attempt {} failed: {}", attempt, lastError);
                if (attempt < MAX_RETRIES) {
                    long delay = RETRY_DELAYS_MS[attempt - 1];
                    log.info("Retrying in {} ms...", delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        throw new RuntimeException("Eroare internă la procesarea AI după " + MAX_RETRIES + " încercări. Ultima eroare: " + lastError);
    }

    @Transactional
    public ExtractionResponse process(ExtractionRequest request) {
        String jsonResult = callExtractionWithRetry(request.getRawText());

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

            LocalDate transactionDate = NormalizerUtil.normalizeDate(request.getRawText());
            String category = root.path("category").asText("Altele");
            String location = root.path("location").asText("Necunoscut");
            String person = root.path("person").asText("Familie");

            ExpenseEntity entity = ExpenseEntity.builder()
                    .amount(amount)
                    .category(category)
                    .location(location)
                    .person(person)
                    .date(transactionDate)
                    .rawInput(request.getRawText())
                    .build();

            syncService.syncExpense(entity);

            return ExtractionResponse.builder()
                    .amount(amount)
                    .category(category)
                    .location(location)
                    .person(person)
                    .transactionDate(transactionDate)
                    .rawInput(request.getRawText())
                    .build();

        } catch (AmountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing extraction JSON", e);
            throw new RuntimeException("Eroare internă la procesarea AI", e);
        }
    }

    public String validateOcrContent(String rawOcrText) {
        ExtractionAssistant assistant = AiServices.create(ExtractionAssistant.class, chatLanguageModel);
        return assistant.validateOcr(rawOcrText);
    }
}
