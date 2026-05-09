package com.familie.cheltuieli_familie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familie.cheltuieli_familie.exception.AiServiceException;
import com.familie.cheltuieli_familie.exception.AmountNotFoundException;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.dto.ExtractionRequest;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.util.NormalizerUtil;
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
import java.util.ArrayList;
import java.util.List;

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
            Ești un expert în extragerea și analizarea datelor financiare din documente și texte în limba română (bonuri fiscale, facturi, chitanțe, mesaje tip text, transcrieri audio).
            
            Sarcina ta este să extragi o LISTĂ de cheltuieli din textul furnizat. 
            Dacă textul pare a fi o transcriere audio a unei conversații între mai multe persoane (ex: Eu și Maria), realizează o diarizare semantică:
            - Folosește contextul, pronumele și conjugarea verbelor pentru a identifica cine a făcut cheltuiala.
            - "Eu" se referă la vorbitorul principal (utilizatorul).
            - "Maria" se referă la soția/partenera menționată.
            - Atribuie corect câmpul "person" fiecărei cheltuieli identified ("Eu", "Maria" sau "Familie").

            Pentru fiecare cheltuială, extrage:
            1. Suma totală (amount).
            2. Categoria cheltuielii (category).
            3. Locația/Comerciantul (location).
            4. Persoana (person) - OBLIGATORIU: "Eu", "Maria" sau "Familie".
            5. Data tranzacției (transactionDate).
            6. O listă cu articolele individuale identificate (items).

            Răspunde DOAR cu un obiect JSON care conține un array "expenses", fără text suplimentar.
            Format JSON obligatoriu:
            {
              "expenses": [
                {
                  "amount": "număr zecimal",
                  "category": "string",
                  "location": "string",
                  "person": "string (Eu/Maria/Familie)",
                  "transactionDate": "YYYY-MM-DD",
                  "items": [ { "name": "string", "price": "zecimal" } ]
                }
              ]
            }
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
                        throw new AiServiceException("Retry interrupted", ie);
                    }
                }
            }
        }

        throw new AiServiceException("Eroare internă la procesarea AI după " + MAX_RETRIES + " încercări. Ultima eroare: " + lastError);
    }

    @Transactional
    public List<ExtractionResponse> process(ExtractionRequest request) {
        String jsonResult = callExtractionWithRetry(request.getRawText());

        try {
            JsonNode root = objectMapper.readTree(jsonResult);
            JsonNode expensesNode = root.path("expenses");
            List<ExtractionResponse> responses = new ArrayList<>();

            if (expensesNode.isArray()) {
                for (JsonNode node : expensesNode) {
                    responses.add(mapToResponse(node, request.getRawText()));
                }
            } else {
                // Fallback if AI returns a single object instead of array or unexpected format
                if (root.has("amount")) {
                    responses.add(mapToResponse(root, request.getRawText()));
                } else if (root.has("expenses") && root.get("expenses").isObject()) {
                     responses.add(mapToResponse(root.get("expenses"), request.getRawText()));
                }
            }

            if (responses.isEmpty()) {
                throw new AmountNotFoundException("Nu s-au identificat cheltuieli în textul furnizat.");
            }

            return responses;

        } catch (AmountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing extraction JSON", e);
            throw new AiServiceException("Eroare internă la procesarea AI", e);
        }
    }

    private ExtractionResponse mapToResponse(JsonNode node, String rawText) {
        // Extract amount with normalization
        String amountStr = node.path("amount").asText();
        BigDecimal amount = NormalizerUtil.normalizeAmount(amountStr);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            // Only attempt global normalization if this is the only node or if it's really missing
            amount = NormalizerUtil.normalizeAmount(node.toString());
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Could not find amount in expense node: {}", node);
            throw new AmountNotFoundException("Nu s-a putut identifica suma unei tranzacții.");
        }

        LocalDate transactionDate = NormalizerUtil.normalizeDate(rawText);
        String category = node.path("category").asText("Altele");
        String location = node.path("location").asText("Necunoscut");
        String person = node.path("person").asText("Familie");

        // Consistency Validation
        JsonNode itemsNode = node.path("items");
        BigDecimal itemsTotal = BigDecimal.ZERO;
        int itemsCount = 0;
        if (itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                BigDecimal price = NormalizerUtil.normalizeAmount(item.path("price").asText("0"));
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    itemsTotal = itemsTotal.add(price);
                    itemsCount++;
                }
            }
        }

        String validationNote = null;
        if (itemsCount > 0) {
            BigDecimal diff = amount.subtract(itemsTotal).abs();
            if (diff.compareTo(new BigDecimal("0.10")) > 0) {
                validationNote = String.format("[AVERTISMENT: Suma celor %d articole (%s) nu corespunde cu totalul (%s)]", 
                        itemsCount, itemsTotal, amount);
            } else {
                validationNote = String.format("[VALIDARE REUȘITĂ: Suma celor %d articole corespunde cu totalul]", itemsCount);
            }
        }

        String finalRawInput = rawText;
        if (validationNote != null) {
            finalRawInput = validationNote + "\n" + finalRawInput;
        }

        ExpenseEntity entity = ExpenseEntity.builder()
                .amount(amount)
                .category(category)
                .location(location)
                .person(person)
                .date(transactionDate)
                .rawInput(finalRawInput.length() > 1000 ? finalRawInput.substring(0, 999) : finalRawInput)
                .build();

        syncService.syncExpense(entity);

        return ExtractionResponse.builder()
                .amount(amount)
                .category(category)
                .location(location)
                .person(person)
                .transactionDate(transactionDate)
                .rawInput(rawText)
                .validationNote(validationNote)
                .build();
    }

    public String validateOcrContent(String rawOcrText) {
        ExtractionAssistant assistant = AiServices.create(ExtractionAssistant.class, chatLanguageModel);
        return assistant.validateOcr(rawOcrText);
    }
}
