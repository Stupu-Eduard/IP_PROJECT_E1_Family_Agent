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

            // Default to text response via existing RAG pipeline
            String textAnswer = ragRetrievalService.askWithContext(userMessage);
            return toTextResponse(textAnswer);

        } catch (Exception e) {
            log.warn("Chart pipeline failed for query '{}', falling back to text RAG: {}",
                    userMessage, e.getMessage());
            String textAnswer = ragRetrievalService.askWithContext(userMessage);
            return toTextResponse(textAnswer);
        }
    }

    private TextResponseDTO toTextResponse(String textAnswer) {
        if (textAnswer == null || textAnswer.isBlank()) {
            log.error("RAG pipeline returned null or blank text answer");
            return new TextResponseDTO(
                    "Nu am putut genera un răspuns. Încearcă din nou sau reformulează întrebarea.");
        }
        return new TextResponseDTO(textAnswer);
    }
}
