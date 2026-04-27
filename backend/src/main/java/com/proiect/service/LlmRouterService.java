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
    private final RetrievalAugmentor retrievalAugmentor;

    private final Map<ChatLanguageModel, LlmConfig.RagAssistant> assistantCache = new ConcurrentHashMap<>();

    public LlmRouterService(LlmConfig.RouterAssistant routerAssistant,
                            @Qualifier("deepseekModel") ChatLanguageModel deepseekModel,
                            RetrievalAugmentor retrievalAugmentor) {
        this.routerAssistant = routerAssistant;
        this.deepseekModel = deepseekModel;
        this.retrievalAugmentor = retrievalAugmentor;
    }

    public String routeAndChat(String userMessage) {
        String classification = routerAssistant.classify(userMessage);
        log.info("Query classification: {}", classification);

        // All queries routed through DeepSeek
        log.info("Routing to DeepSeek ({} query)", classification.trim());

        LlmConfig.RagAssistant assistant = assistantCache.computeIfAbsent(deepseekModel, model -> 
            AiServices.builder(LlmConfig.RagAssistant.class)
                    .chatLanguageModel(model)
                    .retrievalAugmentor(retrievalAugmentor)
                    .build()
        );

        return assistant.chat(userMessage);
    }
}
