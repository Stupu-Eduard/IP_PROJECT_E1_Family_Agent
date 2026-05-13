package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.config.LlmConfig;
import com.familie.cheltuieli_familie.dto.response.AgentResponseDTO;
import com.familie.cheltuieli_familie.dto.response.TextResponseDTO;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentChatService {

    private final VisualIntentExtractor visualIntentExtractor;
    private final ChartGenerationService chartGenerationService;
    private final RagRetrievalService ragRetrievalService;
    private final LlmConfig.ConversationAssistant conversationAssistant;

    public AgentResponseDTO processQuery(String userMessage) {
        try {
            ChartQueryIntent intent = visualIntentExtractor.extract(userMessage);
            log.info("Extracted intent: type={}, chartType={}, groupBy={}",
                    intent.getResponseType(), intent.getChartType(), intent.getGroupBy());

            if ("chart".equalsIgnoreCase(intent.getResponseType())) {
                return chartGenerationService.generate(intent);
            }

            if ("conversation".equalsIgnoreCase(intent.getResponseType())) {
                String reply = conversationAssistant.chat(userMessage);
                return new TextResponseDTO(reply);
            }

            // Default: data_query via hybrid RAG pipeline
            String textAnswer = ragRetrievalService.askWithHybridContext(userMessage);
            return new TextResponseDTO(textAnswer);

        } catch (Exception e) {
            log.warn("Intent extraction failed for query '{}', falling back to hybrid RAG: {}",
                    userMessage, e.getMessage());
            String textAnswer = ragRetrievalService.askWithHybridContext(userMessage);
            return new TextResponseDTO(textAnswer);
        }
    }
}
