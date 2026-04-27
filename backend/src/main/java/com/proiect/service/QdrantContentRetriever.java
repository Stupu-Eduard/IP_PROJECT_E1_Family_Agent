package com.proiect.service;

import com.proiect.dto.EmbeddedExpense;
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

    @Override
    public List<Content> retrieve(Query query) {
        log.info("RAG retrieving content for query: {}", query.text());
        
        List<EmbeddedExpense> results = qdrantVectorService.searchSimilar(query.text(), 20);
        
        if (results.isEmpty()) {
            return List.of();
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
}
