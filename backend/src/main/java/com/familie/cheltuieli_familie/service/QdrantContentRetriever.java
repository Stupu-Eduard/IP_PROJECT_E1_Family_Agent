package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QdrantContentRetriever implements ContentRetriever {

    private final QdrantVectorService qdrantVectorService;

    private static final double MIN_SCORE_THRESHOLD = 0.5;

    @Override
    public List<Content> retrieve(Query query) {
        String cleanQuery = cleanQueryText(query.text());
        log.info("RAG retrieving content for query: {}", cleanQuery);
        
        List<EmbeddedExpense> results = qdrantVectorService.searchSimilar(cleanQuery, 20);
        
        if (results.isEmpty()) {
            return List.of();
        }

        return results.stream()
                .filter(r -> r.getScore() >= MIN_SCORE_THRESHOLD)
                .sorted(Comparator.comparingDouble(EmbeddedExpense::getScore).reversed())
                .limit(5)
                .map(r -> {
                    String text = r.getRawInput() != null ? r.getRawInput()
                            : String.format("%s RON for %s at %s on %s",
                                    r.getAmount(), r.getCategory(), r.getLocation(), r.getDate());
                    return Content.from(TextSegment.from(text));
                })
                .toList();
    }

    /**
     * Cleans query text by removing stop words and extra whitespace before embedding.
     */
    private String cleanQueryText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String cleaned = text.trim();
        // Remove leading/trailing quotes that may come from JSON parsing
        cleaned = cleaned.replaceAll("^['\"]+", "").replaceAll("['\"]+$", "");
        // Remove common Romanian stop words for better semantic search
        cleaned = cleaned.replaceAll("(?i)\\b(salut|buna|te rog|poti sa|mi spui|ceva despre|am adaugat|o cheltuiala)\\b", "");
        // Collapse multiple spaces
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }
}
