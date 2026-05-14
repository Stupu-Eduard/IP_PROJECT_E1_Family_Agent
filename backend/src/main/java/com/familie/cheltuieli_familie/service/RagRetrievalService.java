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
     * Performs a RAG query: retrieves context from vector store and asks the LLM via Router.
     * The retrieved context is injected into the prompt for the LLM to use.
     *
     * @param query The user query.
     * @return The LLM's answer based on the retrieved context.
     */
    public String askWithContext(String query) {
        log.info("Performing RAG query: {}", query);

        // Retrieve relevant context from Qdrant
        String context = retrieveContext(query, 10);
        log.info("Retrieved context for query (length={}): {}", context.length(),
                context.substring(0, Math.min(200, context.length())));

        // Build augmented query with context
        String augmentedQuery = buildAugmentedQuery(query, context);

        return llmRouterService.routeAndChat(augmentedQuery);
    }

    private String buildAugmentedQuery(String query, String context) {
        if (context == null || context.isBlank() ||
                context.equals("Nu s-au găsit cheltuieli relevante în baza de date.")) {
            log.warn("No relevant context found for query: {}", query);
            return query + "\n\n[NOTA: Nu există date relevante în istoric pentru această întrebare.]";
        }

        return String.format("""
            Context din baza de date (cheltuieli relevante):
            %s

            Întrebare utilizator: %s

            Răspunde folosind contextul de mai sus. Dacă informația nu este în context, spune că nu ai date suficiente.
            """, context, query);
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
