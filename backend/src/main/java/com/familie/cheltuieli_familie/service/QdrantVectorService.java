package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.exception.VectorStoreException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QdrantVectorService {

    private static final String KEY_CATEGORY = "category";
    private static final String KEY_PERSON = "person";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_DATE = "date";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_ID = "id";
    private static final String QDRANT_RESULT = "result";
    private static final String QDRANT_VALUE_KEY = "value";

    private final QdrantEmbeddingStore embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final RestTemplate restTemplate;

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.port:6333}")
    private int httpPort;

    @Value("${qdrant.collection-name:expenses}")
    private String collectionName;

    public QdrantVectorService(QdrantEmbeddingStore embeddingStore, EmbeddingModel embeddingModel, RestTemplate restTemplate) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.restTemplate = restTemplate;
    }

    public void storeExpense(ExpenseEntity expense) {
        log.info("Storing expense ID {} in vector store", expense.getId());
        String textToEmbed = expense.getRawInput();
        if (textToEmbed == null || textToEmbed.isEmpty()) {
            textToEmbed = String.format("Cheltuială: %s, Sumă: %s, Persoană: %s, Locație: %s, Dată: %s",
                    expense.getCategory(), expense.getAmount(), expense.getPerson(), expense.getLocation(), expense.getDate());
        }

        Metadata metadata = new Metadata();
        metadata.put(KEY_ID, expense.getId());
        metadata.put(KEY_AMOUNT, expense.getAmount().doubleValue());
        if (expense.getCategory() != null) metadata.put(KEY_CATEGORY, expense.getCategory());
        if (expense.getPerson() != null) metadata.put(KEY_PERSON, expense.getPerson());
        if (expense.getLocation() != null) metadata.put(KEY_LOCATION, expense.getLocation());
        if (expense.getDate() != null) metadata.put(KEY_DATE, expense.getDate().toString());

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

    @SuppressWarnings("unchecked")
    public List<EmbeddedExpense> searchWithFilter(
            String query, int topK, String category, String person, LocalDate from, LocalDate to) {

        log.info("Searching vector store for query: '{}', topK: {}, category: {}, person: {}", query, topK, category, person);
        
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        // Build REST request body
        Map<String, Object> body = new HashMap<>();
        body.put("vector", queryEmbedding.vector());
        body.put("limit", topK);
        body.put("with_vector", false);
        body.put("with_payload", true);

        // Build filter
        Map<String, Object> filter = buildQdrantFilter(category, person, from, to);
        if (filter != null) {
            body.put("filter", filter);
        }

        String url = String.format("http://%s:%d/collections/%s/points/search", host, httpPort, collectionName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get(QDRANT_RESULT);
                if (results != null) {
                    return results.stream()
                            .map(this::mapRestResultToEmbeddedExpense)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.error("Qdrant search failed: {}", e.getMessage());
        }

        return List.of();
    }

    private Map<String, Object> buildQdrantFilter(String category, String person, LocalDate from, LocalDate to) {
        List<Map<String, Object>> conditions = new ArrayList<>();

        if (category != null && !category.isEmpty()) {
            conditions.add(Map.of("key", KEY_CATEGORY, "match", Map.of(QDRANT_VALUE_KEY, category)));
        }
        if (person != null && !person.isEmpty()) {
            conditions.add(Map.of("key", KEY_PERSON, "match", Map.of(QDRANT_VALUE_KEY, person)));
        }
        if (from != null) {
            conditions.add(Map.of("key", KEY_DATE, "range", Map.of("gte", from.toString())));
        }
        if (to != null) {
            conditions.add(Map.of("key", KEY_DATE, "range", Map.of("lte", to.toString())));
        }

        if (conditions.isEmpty()) {
            return null;
        }
        if (conditions.size() == 1) {
            return conditions.get(0);
        }
        return Map.of("must", conditions);
    }

    @SuppressWarnings("unchecked")
    private EmbeddedExpense mapRestResultToEmbeddedExpense(Map<String, Object> result) {
        Map<String, Object> payload = (Map<String, Object>) result.get("payload");
        double score = ((Number) result.get("score")).doubleValue();

        Long id = null;
        if (payload != null && payload.get(KEY_ID) != null) {
            Object idObj = payload.get(KEY_ID);
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else {
                try {
                    id = Long.parseLong(idObj.toString());
                } catch (Exception ignored) {}
            }
        }

        BigDecimal amount = null;
        if (payload != null && payload.get(KEY_AMOUNT) != null) {
            Object amtObj = payload.get(KEY_AMOUNT);
            if (amtObj instanceof Number) {
                amount = BigDecimal.valueOf(((Number) amtObj).doubleValue());
            } else {
                try {
                    amount = new BigDecimal(amtObj.toString());
                } catch (Exception ignored) {}
            }
        }

        return EmbeddedExpense.builder()
                .id(id)
                .amount(amount)
                .category(payload != null ? (String) payload.get(KEY_CATEGORY) : null)
                .person(payload != null ? (String) payload.get(KEY_PERSON) : null)
                .location(payload != null ? (String) payload.get(KEY_LOCATION) : null)
                .date(parseLocalDate(payload != null ? (String) payload.get(KEY_DATE) : null))
                .rawInput(payload != null ? (String) payload.get("text_segment") : null)
                .score(score)
                .build();
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean existsInVectorStore(Long id) {
        Map<String, Object> body = new HashMap<>();
        body.put("vector", new float[2048]);
        body.put("limit", 1);
        body.put("with_vector", false);
        body.put("with_payload", true);
        body.put("filter", Map.of("must", List.of(Map.of("key", KEY_ID, "match", Map.of(QDRANT_VALUE_KEY, id.toString())))));

        String url = String.format("http://%s:%d/collections/%s/points/search", host, httpPort, collectionName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get(QDRANT_RESULT);
                return results != null && !results.isEmpty();
            }
        } catch (Exception e) {
            log.error("Qdrant exists check failed: {}", e.getMessage());
        }
        return false;
    }
}
