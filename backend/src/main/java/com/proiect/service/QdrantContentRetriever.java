package com.proiect.service;

import com.proiect.dto.EmbeddedExpense;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class QdrantContentRetriever implements ContentRetriever {

    private final QdrantVectorService qdrantVectorService;
    private final ScoringModel scoringModel;

    @Override
    public List<Content> retrieve(Query query) {
        log.info("RAG retrieving content for query: {}", query.text());
        
        // Retrieve more results initially for re-ranking
        List<EmbeddedExpense> results = qdrantVectorService.searchSimilar(query.text(), 20);
        
        if (results.isEmpty()) {
            return List.of();
        }

        // Re-ranking
        List<TextSegment> segmentsToScore = results.stream()
                .map(expense -> TextSegment.from(formatExpenseForScoring(expense)))
                .collect(Collectors.toList());

        List<Double> scores = scoringModel.scoreAll(segmentsToScore, query.text()).content();

        for (int i = 0; i < results.size(); i++) {
            results.get(i).setScore(scores.get(i));
        }

        return results.stream()
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
