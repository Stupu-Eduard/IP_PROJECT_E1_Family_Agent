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

    public LlmRouterService(LlmConfig.RouterAssistant routerAssistant,
                            LlmConfig.RagAssistant ragAssistant) {
        this.ragAssistant = ragAssistant;
    }

    public String routeAndChat(String userMessage) {
        // Route directly to RagAssistant which has both RAG retrieval + DB tools
        log.info("Routing query to RagAssistant: {}", userMessage);

        String response = ragAssistant.chat(userMessage);
        if (response == null || response.isBlank()) {
            log.error("RagAssistant returned null or blank response for message: {}", userMessage);
        }
        return response;
    }
}
