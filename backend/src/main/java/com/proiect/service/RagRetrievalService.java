package com.proiect.service;

import com.proiect.config.LlmConfig;
import com.proiect.dto.EmbeddedExpense;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagRetrievalService {

    private final QdrantVectorService qdrantVectorService;
    private final LlmConfig.RagAssistant ragAssistant;

    /**
     * Performs a RAG query: automatically retrieves context and asks the LLM.
     *
     * @param query The user query.
     * @return The LLM's answer based on the retrieved context.
     */
    public String askWithContext(String query) {
        log.info("Performing RAG query via automated RetrievalAugmentor: {}", query);
        return ragAssistant.chat(query);
    }

    /**
     * Retrieves relevant expenses and formats them as a context string (Utility method).
     *
     * @param query The user query.
     * @param topK  Number of relevant results to retrieve.
     * @return A formatted string containing the retrieved context.
     */
    public String retrieveContext(String query, int topK) {
        log.info("Retrieving context for query: {}", query);
        List<EmbeddedExpense> results = qdrantVectorService.searchSimilar(query, topK);

        if (results.isEmpty()) {
            return "Nu s-au găsit cheltuieli relevante în baza de date.";
        }

        StringBuilder context = new StringBuilder("Cheltuieli anterioare relevante:\n");
        for (int i = 0; i < results.size(); i++) {
            EmbeddedExpense expense = results.get(i);
            context.append(String.format("%d. %s: %.2f RON la %s pe data de %s (Persoană: %s, Text original: %s)\n",
                    i + 1,
                    expense.getCategory(),
                    expense.getAmount(),
                    expense.getLocation() != null ? expense.getLocation() : "Necunoscut",
                    expense.getDate() != null ? expense.getDate().toString() : "Necunoscut",
                    expense.getPerson() != null ? expense.getPerson() : "Familie",
                    expense.getRawInput()
            ));
        }

        return context.toString();
    }
}
