package com.proiect.m3.extraction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proiect.m3.extraction.exception.AmountNotFoundException;
import com.proiect.m3.extraction.model.ExpenseEntity;
import com.proiect.m3.extraction.model.ExtractionRequest;
import com.proiect.m3.extraction.model.ExtractionResponse;
import com.proiect.m3.extraction.repository.ExpenseRepository;
import com.proiect.m3.extraction.util.NormalizerUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ExtractionService {

    private static final Logger logger = Logger.getLogger(ExtractionService.class.getName());
    private final ChatLanguageModel chatLanguageModel;
    private final ExpenseRepository expenseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExtractionService(ChatLanguageModel chatLanguageModel, ExpenseRepository expenseRepository) {
        this.chatLanguageModel = chatLanguageModel;
        this.expenseRepository = expenseRepository;
    }

    interface ExtractionAssistant {
        @SystemMessage("Ești un asistent financiar expert în extragerea datelor din cheltuieli familiale. " +
                "Extrage din text următoarele entități: amount (Double), currency (RON/EUR/USD), category (mâncare, transport, cărți, sănătate, divertisment, etc.), location (magazin, oraș), person. " +
                "Dacă suma nu este numerică, păstrează textul original pentru normalizare. " +
                "Returnează EXCLUSIV un obiect JSON valid.")
        String extract(@UserMessage String text);

        @SystemMessage("Ești un expert în validare OCR. Verifică dacă suma prețurilor articolelor individuale dintr-un bon coincide cu totalul extras. " +
                "Răspunde doar cu 'VALID' sau 'INVALID: [motiv]'.")
        String validateOcr(@UserMessage String rawOcrText);
    }

    @Transactional
    public ExtractionResponse process(ExtractionRequest request) {
        ExtractionAssistant assistant = AiServices.create(ExtractionAssistant.class, chatLanguageModel);
        
        String jsonResult = assistant.extract(request.getRawText());
        logger.info("AI Raw Extraction: " + jsonResult);

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

            ExpenseEntity entity = ExpenseEntity.builder()
                    .amount(amount)
                    .category(category)
                    .location(location)
                    .person(person)
                    .transactionDate(transactionDate)
                    .date(transactionDate) // Unified date field
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
            logger.log(Level.SEVERE, "Error processing extraction", e);
            if (e instanceof AmountNotFoundException) throw (AmountNotFoundException) e;
            throw new RuntimeException("Eroare internă la procesarea AI", e);
        }
    }

    public String validateOcrContent(String rawOcrText) {
        ExtractionAssistant assistant = AiServices.create(ExtractionAssistant.class, chatLanguageModel);
        return assistant.validateOcr(rawOcrText);
    }
}
