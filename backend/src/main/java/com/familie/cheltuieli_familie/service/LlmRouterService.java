package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.config.LlmConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LlmRouterService {

    private final LlmConfig.RagAssistant ragAssistant;
    private final HallucinationGuard hallucinationGuard;

    public LlmRouterService(LlmConfig.RagAssistant ragAssistant, HallucinationGuard hallucinationGuard) {
        this.ragAssistant = ragAssistant;
        this.hallucinationGuard = hallucinationGuard;
    }

    public String routeAndChat(String userMessage) {
        log.info("Routing query to RagAssistant: {}", userMessage);

        String response = ragAssistant.chat(userMessage);
        if (response == null || response.isBlank()) {
            log.error("RagAssistant returned null or blank response for message: {}", userMessage);
            return response;
        }

        // Validate response for hallucinations
        return hallucinationGuard.validate(response, userMessage);
    }
}
