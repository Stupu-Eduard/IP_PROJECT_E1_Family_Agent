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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class QdrantVectorService {

    private static final String KEY_CATEGORY = "category";
    private static final String KEY_PERSON = "person";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_DATE = "date";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_ID = "expense_id";
    private static final String KEY_RAW_INPUT = "raw_input";
    private static final String KEY_FAMILY_ID = "family_id";
    private static final String KEY_USER_ID = "user_id";
    private static final String QDRANT_RESULT = "result";
    private static final String MATCH = "match";
    private static final String VALUE = "value";
    private static final String SCORE_FIELD = "score";

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
        metadata.put(KEY_RAW_INPUT, textToEmbed);
        if (expense.getCategory() != null) metadata.put(KEY_CATEGORY, expense.getCategory());
        if (expense.getPerson() != null) metadata.put(KEY_PERSON, expense.getPerson());
        if (expense.getLocation() != null) metadata.put(KEY_LOCATION, expense.getLocation());
        if (expense.getDate() != null) metadata.put(KEY_DATE, expense.getDate().toString());
        if (expense.getFamilyId() != null) metadata.put(KEY_FAMILY_ID, expense.getFamilyId());
        if (expense.getUserId() != null) metadata.put(KEY_USER_ID, expense.getUserId());

        try {
            Document document = Document.from(textToEmbed, metadata);
            List<TextSegment> segments = DocumentSplitters.recursive(1000, 100).split(document);

            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }
            log.info("Stored {} segments for expense ID {}", segments.size(), expense.getId());
        } catch (Exception e) {
            log.error("Failed to embed/store expense ID {}: {}", expense.getId(), e.getMessage());
            throw new VectorStoreException("Embedding failed for expense " + expense.getId(), e);
        }
    }

    public List<EmbeddedExpense> searchSimilar(String query, int topK) {
        return searchWithFilter(query, topK, new SearchFilter(null, null, null, null, null, null));
    }

    public List<EmbeddedExpense> searchSimilar(String query, int topK, Long familyId, Long userId) {
        return searchWithFilter(query, topK, new SearchFilter(null, null, null, null, familyId, userId));
    }

    public List<EmbeddedExpense> searchWithFilter(
            String query, int topK, String category, String person, LocalDate from, LocalDate to) {
        return searchWithFilter(query, topK, new SearchFilter(category, person, from, to, null, null));
    }

    public List<EmbeddedExpense> searchWithFilter(
            String query, int topK, String category, String person, LocalDate from, LocalDate to,
            Long familyId, Long userId) {
        return searchWithFilter(query, topK, new SearchFilter(category, person, from, to, familyId, userId));
    }

    public List<EmbeddedExpense> searchWithFilter(String query, int topK, SearchFilter filter) {

        log.info("Searching vector store for query: '{}', topK: {}, category: {}, person: {}, familyId: {}, userId: {}",
                query, topK, filter.category(), filter.person(), filter.familyId(), filter.userId());
        
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        // Build REST request body
        Map<String, Object> body = new HashMap<>();
        body.put("vector", queryEmbedding.vector());
        body.put("limit", topK);
        body.put("with_vector", false);
        body.put("with_payload", true);

        // Build filter
        Map<String, Object> filterMap = buildQdrantFilter(filter);
        if (!filterMap.isEmpty()) {
            body.put("filter", filterMap);
        }

        List<Map<String, Object>> results = executeQdrantSearch(body);
        if (results != null) {
            if (!results.isEmpty()) {
                double minScore = results.stream()
                        .mapToDouble(r -> ((Number) r.get(SCORE_FIELD)).doubleValue())
                        .min().orElse(0.0);
                double maxScore = results.stream()
                        .mapToDouble(r -> ((Number) r.get(SCORE_FIELD)).doubleValue())
                        .max().orElse(0.0);
                double avgScore = results.stream()
                        .mapToDouble(r -> ((Number) r.get(SCORE_FIELD)).doubleValue())
                        .average().orElse(0.0);
                log.info("Qdrant returned {} results, score range: {} - {}, avg: {}",
                        results.size(), String.format("%.4f", minScore), String.format("%.4f", maxScore),
                        String.format("%.4f", avgScore));
            } else {
                log.info("Qdrant returned 0 results for query: '{}'", query);
            }
            return results.stream()
                    .map(this::mapRestResultToEmbeddedExpense)
                    .toList();
        }
        return List.of();
    }

    private Map<String, Object> buildQdrantFilter(SearchFilter filter) {
        List<Map<String, Object>> conditions = new ArrayList<>();

        if (filter.category() != null && !filter.category().isEmpty()) {
            conditions.add(Map.of("key", KEY_CATEGORY, MATCH, Map.of(VALUE, filter.category())));
        }
        if (filter.person() != null && !filter.person().isEmpty()) {
            conditions.add(Map.of("key", KEY_PERSON, MATCH, Map.of(VALUE, filter.person())));
        }
        if (filter.from() != null) {
            conditions.add(Map.of("key", KEY_DATE, "range", Map.of("gte", filter.from().toString())));
        }
        if (filter.to() != null) {
            conditions.add(Map.of("key", KEY_DATE, "range", Map.of("lte", filter.to().toString())));
        }
        if (filter.familyId() != null) {
            conditions.add(Map.of("key", KEY_FAMILY_ID, MATCH, Map.of(VALUE, filter.familyId())));
        }
        if (filter.userId() != null && filter.familyId() == null) {
            conditions.add(Map.of("key", KEY_USER_ID, MATCH, Map.of(VALUE, filter.userId())));
        }

        if (conditions.isEmpty()) {
            return Map.of();
        }
        if (conditions.size() == 1) {
            return conditions.get(0);
        }
        return Map.of("must", conditions);
    }

    private EmbeddedExpense mapRestResultToEmbeddedExpense(Map<String, Object> result) {
        Map<String, Object> payload = (Map<String, Object>) result.get("payload");
        double score = ((Number) result.get(SCORE_FIELD)).doubleValue();

        Long id = null;
        if (payload != null && payload.get(KEY_ID) != null) {
            Object idObj = payload.get(KEY_ID);
            if (idObj instanceof Number number) {
                id = number.longValue();
            } else {
                id = Long.parseLong(idObj.toString());
            }
        }

        BigDecimal amount = null;
        if (payload != null && payload.get(KEY_AMOUNT) != null) {
            Object amtObj = payload.get(KEY_AMOUNT);
            if (amtObj instanceof Number number) {
                amount = BigDecimal.valueOf(number.doubleValue());
            } else {
                amount = new BigDecimal(amtObj.toString());
            }
        }

        return EmbeddedExpense.builder()
                .id(id)
                .amount(amount)
                .category(payload != null ? (String) payload.get(KEY_CATEGORY) : null)
                .person(payload != null ? (String) payload.get(KEY_PERSON) : null)
                .location(payload != null ? (String) payload.get(KEY_LOCATION) : null)
                .date(parseLocalDate(payload != null ? (String) payload.get(KEY_DATE) : null))
                .rawInput(payload != null ? (String) payload.get(KEY_RAW_INPUT) : null)
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

    private List<Map<String, Object>> executeQdrantSearch(Map<String, Object> body) {
        String url = String.format("http://%s:%d/collections/%s/points/search", host, httpPort, collectionName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {});
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get(QDRANT_RESULT);
            }
        } catch (Exception e) {
            log.error("Qdrant search failed: {}", e.getMessage());
        }
        return List.of();
    }

    public boolean existsInVectorStore(Long id) {
        String url = String.format("http://%s:%d/collections/%s/points/scroll", host, httpPort, collectionName);
        try {
            Map<String, Object> filter = Map.of(
                "must", List.of(Map.of("key", KEY_ID, MATCH, Map.of(VALUE, id.toString())))
            );
            Map<String, Object> body = Map.of("filter", filter, "limit", 1, "with_payload", false);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {});
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = (Map<String, Object>) response.getBody().get(QDRANT_RESULT);
                List<Map<String, Object>> points = result != null ? (List<Map<String, Object>>) result.get("points") : null;
                return points != null && !points.isEmpty();
            }
        } catch (Exception e) {
            log.debug("Point with id {} not found in collection {}: {}", id, collectionName, e.getMessage());
        }
        return false;
    }

    public record SearchFilter(String category, String person, LocalDate from, LocalDate to, Long familyId, Long userId) {
    }
}
