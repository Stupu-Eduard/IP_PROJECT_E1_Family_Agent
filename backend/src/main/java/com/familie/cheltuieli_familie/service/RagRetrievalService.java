package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagRetrievalService {

    private final QdrantVectorService qdrantVectorService;
    private final LlmRouterService llmRouterService;

    /**
     * Performs a RAG query: asks the LLM via Router.
     * The LLM has access to both Qdrant retrieval (via RetrievalAugmentor)
     * and relational DB tools (via ExpenseTools), so we pass the raw query.
     *
     * @param query The user query.
     * @return The LLM's answer based on retrieved context and/or DB tools.
     */
    public String askWithContext(String query) {
        log.info("Performing RAG query: {}", query);
        return llmRouterService.routeAndChat(query);
    }

    /**
     * Retrieves relevant expenses and formats them.
     *
     * @param query The user query.
     * @param topK  Number of results to retrieve.
     * @return A formatted string containing the top results.
     */
    public String retrieveContext(String query, int topK) {
        log.info("Retrieving context for query: {}", query);
        List<EmbeddedExpense> results = qdrantVectorService.searchSimilar(query, topK);

        if (results.isEmpty()) {
            return "Nu s-au găsit cheltuieli relevante în baza de date.";
        }

        // Sort by score and take top 5
        List<EmbeddedExpense> topResults = results.stream()
                .sorted(Comparator.comparingDouble(EmbeddedExpense::getScore).reversed())
                .limit(5)
                .toList();

        StringBuilder context = new StringBuilder("Cheltuieli anterioare relevante:\n");
        for (int i = 0; i < topResults.size(); i++) {
            EmbeddedExpense expense = topResults.get(i);
            context.append(String.format("%d. %s: %.2f RON la %s pe data de %s (Persoană: %s, Scor: %.4f)%n",
                    i + 1,
                    expense.getCategory(),
                    expense.getAmount(),
                    expense.getLocation() != null ? expense.getLocation() : "Necunoscut",
                    expense.getDate() != null ? expense.getDate().toString() : "Necunoscut",
                    expense.getPerson() != null ? expense.getPerson() : "Familie",
                    expense.getScore()
            ));
        }

        return context.toString();
    }

}
