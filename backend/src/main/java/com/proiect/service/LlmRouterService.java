package com.proiect.service;

import com.proiect.config.LlmConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LlmRouterService {

    private final LlmConfig.RouterAssistant routerAssistant;
    private final ChatLanguageModel deepseekModel;
    private final ChatLanguageModel claudeModel;
    private final RetrievalAugmentor retrievalAugmentor;

    private final Map<ChatLanguageModel, LlmConfig.RagAssistant> assistantCache = new ConcurrentHashMap<>();

    public LlmRouterService(LlmConfig.RouterAssistant routerAssistant,
                            @Qualifier("deepseekModel") ChatLanguageModel deepseekModel,
                            @Qualifier("claudeModel") ChatLanguageModel claudeModel,
                            RetrievalAugmentor retrievalAugmentor) {
        this.routerAssistant = routerAssistant;
        this.deepseekModel = deepseekModel;
        this.claudeModel = claudeModel;
        this.retrievalAugmentor = retrievalAugmentor;
    }

    public String routeAndChat(String userMessage) {
        String classification = routerAssistant.classify(userMessage);
        log.info("Query classification: {}", classification);

        ChatLanguageModel selectedModel;
        if ("COMPLEX".equalsIgnoreCase(classification.trim())) {
            log.info("Routing to Claude 3.5 Sonnet (Complex query)");
            selectedModel = claudeModel;
        } else {
            log.info("Routing to DeepSeek (Simple query)");
            selectedModel = deepseekModel;
        }

        LlmConfig.RagAssistant assistant = assistantCache.computeIfAbsent(selectedModel, model -> 
            AiServices.builder(LlmConfig.RagAssistant.class)
                    .chatLanguageModel(model)
                    .retrievalAugmentor(retrievalAugmentor)
                    .build()
        );

        return assistant.chat(userMessage);
    }
}
