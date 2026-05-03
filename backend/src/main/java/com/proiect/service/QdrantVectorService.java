package com.proiect.service;

import com.proiect.dto.EmbeddedExpense;
import com.proiect.model.ExpenseEntity;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
@Slf4j
public class QdrantVectorService {

    private static final String AMOUNT_KEY = "amount";
    private static final String CATEGORY_KEY = "category";
    private static final String PERSON_KEY = "person";
    private static final String VALUE_CONST = "value";
    private static final String MATCH_CONST = "match";

    private final QdrantEmbeddingStore embeddingStore;
    private final EmbeddingModel embeddingModel;

    public QdrantVectorService(QdrantEmbeddingStore embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    public void storeExpense(ExpenseEntity expense) {
        log.info("Storing expense ID {} in vector store", expense.getId());
        String textToEmbed = expense.getRawInput();
        if (textToEmbed == null || textToEmbed.isEmpty()) {
            textToEmbed = String.format("Cheltuială: %s, Sumă: %s, Persoană: %s, Locație: %s, Dată: %s",
                    expense.getCategory(), expense.getAmount(), expense.getPerson(), expense.getLocation(), expense.getDate());
        }

        Metadata metadata = new Metadata();
        metadata.put("id", expense.getId());
        metadata.put(AMOUNT_KEY, expense.getAmount().doubleValue());
        if (expense.getCategory() != null) metadata.put(CATEGORY_KEY, expense.getCategory());
        if (expense.getPerson() != null) metadata.put(PERSON_KEY, expense.getPerson());
        if (expense.getLocation() != null) metadata.put("location", expense.getLocation());
        if (expense.getDate() != null) metadata.put("date", expense.getDate().toString());

        Document document = Document.from(textToEmbed, metadata);
        // Use recursive splitter to handle potentially long receipts/OCR text
        List<TextSegment> segments = DocumentSplitters.recursive(1000, 100).split(document);
        
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }
        log.info("Stored {} segments for expense ID {}", segments.size(), expense.getId());
    }

    public List<EmbeddedExpense> searchSimilar(String query, int topK) {
        return searchWithFilter(query, topK, null, null, null, null);
    }

    public List<EmbeddedExpense> searchWithFilter(
            String query, int topK, String category, String person, LocalDate from, LocalDate to) {

        log.info("Searching vector store for query: '{}', topK: {}, category: {}, person: {}", query, topK, category, person);
        
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        Filter filter = null;
        List<Filter> filters = new ArrayList<>();

        if (category != null && !category.isEmpty()) {
            filters.add(metadataKey(CATEGORY_KEY).isEqualTo(category));
        }
        if (person != null && !person.isEmpty()) {
            filters.add(metadataKey(PERSON_KEY).isEqualTo(person));
        }
        if (from != null) {
            filters.add(metadataKey("date").isGreaterThanOrEqualTo(from.toString()));
        }
        if (to != null) {
            filters.add(metadataKey("date").isLessThanOrEqualTo(to.toString()));
        }

        if (!filters.isEmpty()) {
            if (filters.size() == 1) {
                filter = filters.get(0);
            } else {
                // Combine filters with AND logic
                filter = filters.get(0);
                for (int i = 1; i < filters.size(); i++) {
                    filter = Filter.and(filter, filters.get(i));
                }
            }
        }

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .filter(filter)
                .maxResults(topK)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        return searchResult.matches().stream()
                .map(this::mapToEmbeddedExpense)
                .toList();
    }

    private EmbeddedExpense mapToEmbeddedExpense(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Metadata metadata = segment.metadata();

        return EmbeddedExpense.builder()
                .id(metadata.getLong("id"))
                .amount(metadata.getDouble(AMOUNT_KEY) != null ? BigDecimal.valueOf(metadata.getDouble(AMOUNT_KEY)) : null)
                .category(metadata.getString(CATEGORY_KEY))
                .person(metadata.getString(PERSON_KEY))
                .location(metadata.getString("location"))
                .date(parseLocalDate(metadata.getString("date")))
                .rawInput(segment.text())
                .score(match.score())
                .build();
    }

    private LocalDate parseLocalDate(Object obj) {
        if (obj instanceof String s) {
            try {
                return LocalDate.parse(s);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public boolean existsInVectorStore(Long id) {
        // Since Qdrant doesn't have a direct "exists by metadata" in LangChain4j EmbeddingStore easily
        // we can search with a filter for the ID and see if we get anything
        Filter filter = metadataKey("id").isEqualTo(id);
        
        // We need a dummy embedding since search requires one
        Embedding dummyEmbedding = Embedding.from(new float[1536]);
        
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(dummyEmbedding)
                .filter(filter)
                .maxResults(1)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        return !result.matches().isEmpty();
    }
}
