package com.proiect.service;

import com.proiect.dto.EmbeddedExpense;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagRetrievalService {

    private final QdrantVectorService qdrantVectorService;
    private final LlmRouterService llmRouterService;
    private final ScoringModel scoringModel;

    /**
     * Performs a RAG query: automatically retrieves context and asks the LLM via Router.
     *
     * @param query The user query.
     * @return The LLM's answer based on the retrieved context.
     */
    public String askWithContext(String query) {
        log.info("Performing RAG query via LlmRouterService: {}", query);
        return llmRouterService.routeAndChat(query);
    }

    /**
     * Retrieves relevant expenses, re-ranks them using a cross-encoder, and formats them.
     *
     * @param query The user query.
     * @param topK  Number of results to retrieve initially before re-ranking.
     * @return A formatted string containing the top-5 re-ranked context.
     */
    public String retrieveContext(String query, int topK) {
        log.info("Retrieving context for query: {}", query);
        List<EmbeddedExpense> initialResults = qdrantVectorService.searchSimilar(query, topK);

        if (initialResults.isEmpty()) {
            return "Nu s-au găsit cheltuieli relevante în baza de date.";
        }

        // Re-ranking step
        log.info("Re-ranking {} results using ScoringModel", initialResults.size());
        
        List<TextSegment> segmentsToScore = initialResults.stream()
                .map(expense -> TextSegment.from(formatExpenseForScoring(expense)))
                .collect(Collectors.toList());

        List<Double> scores = scoringModel.scoreAll(segmentsToScore, query).content();

        for (int i = 0; i < initialResults.size(); i++) {
            initialResults.get(i).setScore(scores.get(i));
        }

        // Sort by re-ranked score and take top 5
        List<EmbeddedExpense> rerankedResults = initialResults.stream()
                .sorted(Comparator.comparingDouble(EmbeddedExpense::getScore).reversed())
                .limit(5)
                .collect(Collectors.toList());

        StringBuilder context = new StringBuilder("Cheltuieli anterioare relevante (re-rankuite):\n");
        for (int i = 0; i < rerankedResults.size(); i++) {
            EmbeddedExpense expense = rerankedResults.get(i);
            context.append(String.format("%d. %s: %.2f RON la %s pe data de %s (Persoană: %s, Scor re-rank: %.4f)\n",
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

    private String formatExpenseForScoring(EmbeddedExpense expense) {
        return String.format("Categorie: %s, Sumă: %s, Locație: %s, Persoană: %s, Dată: %s, Detalii: %s",
                expense.getCategory(),
                expense.getAmount(),
                expense.getLocation(),
                expense.getPerson(),
                expense.getDate(),
                expense.getRawInput());
    }
}
