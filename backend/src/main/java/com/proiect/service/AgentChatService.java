package com.proiect.service;

import com.proiect.dto.response.AgentResponseDTO;
import com.proiect.dto.response.TextResponseDTO;
import com.proiect.model.ChartQueryIntent;
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
                return chartGenerationService.generate(userMessage, intent);
            }

            // Default to text response via existing RAG pipeline
            String textAnswer = ragRetrievalService.askWithContext(userMessage);
            return new TextResponseDTO(textAnswer);

        } catch (Exception e) {
            log.warn("Chart pipeline failed for query '{}', falling back to text RAG: {}",
                    userMessage, e.getMessage());
            String textAnswer = ragRetrievalService.askWithContext(userMessage);
            return new TextResponseDTO(textAnswer);
        }
    }
}
