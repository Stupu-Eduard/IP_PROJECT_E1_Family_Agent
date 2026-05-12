package com.familie.cheltuieli_familie.service;

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

    public AgentResponseDTO processQuery(String userMessage) {
        try {
            ChartQueryIntent intent = visualIntentExtractor.extract(userMessage);
            log.info("Extracted intent: type={}, chartType={}, groupBy={}",
                    intent.getResponseType(), intent.getChartType(), intent.getGroupBy());

            if ("chart".equalsIgnoreCase(intent.getResponseType())) {
                return chartGenerationService.generate(intent);
            }

            // Default to text response via hybrid RAG pipeline
            String textAnswer = ragRetrievalService.askWithHybridContext(userMessage);
            return new TextResponseDTO(textAnswer);

        } catch (Exception e) {
            log.warn("Chart pipeline failed for query '{}', falling back to hybrid text RAG: {}",
                    userMessage, e.getMessage());
            String textAnswer = ragRetrievalService.askWithHybridContext(userMessage);
            return new TextResponseDTO(textAnswer);
        }
    }
}
