package com.proiect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proiect.model.ExpenseEntity;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

    private final QdrantEmbeddingStore embeddingStore;
    private final EmbeddingService embeddingService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${qdrant.port:6333}")
    private int qdrantPort;

    @Value("${qdrant.collection-name:expenses}")
    private String collectionName;

    public QdrantVectorService(QdrantEmbeddingStore embeddingStore, EmbeddingService embeddingService, RestTemplate restTemplate) {
        this.embeddingStore = embeddingStore;
        this.embeddingService = embeddingService;
        this.restTemplate = restTemplate;
    }

    public void storeExpense(ExpenseEntity expense) {
        log.info("Storing expense ID {} in vector store", expense.getId());
        String textToEmbed = expense.getRawInput();
        if (textToEmbed == null || textToEmbed.isEmpty()) {
            textToEmbed = expense.getCategory();
        }
        if (textToEmbed == null || textToEmbed.isEmpty()) {
            textToEmbed = "Unknown Expense";
        }

        float[] vector = embeddingService.getEmbedding(textToEmbed);
        Embedding embedding = Embedding.from(vector);

        Metadata metadata = new Metadata();
        metadata.add("id", expense.getId());
        metadata.add("amount", expense.getAmount().doubleValue());
        if (expense.getCategory() != null) metadata.add("category", expense.getCategory());
        if (expense.getPerson() != null) metadata.add("person", expense.getPerson());
        if (expense.getDate() != null) metadata.add("date", expense.getDate().toString());

        TextSegment segment = TextSegment.from(textToEmbed, metadata);

        embeddingStore.add(embedding, segment);
    }

    public List<com.proiect.dto.EmbeddedExpense> searchSimilar(String query, int topK) {
        return searchWithFilter(query, topK, null, null, null, null);
    }

    public List<com.proiect.dto.EmbeddedExpense> searchWithFilter(
            String query, int topK, String category, String person, LocalDate from, LocalDate to) {

        log.info("Searching vector store for query: '{}', topK: {}", query, topK);
        float[] queryVector = embeddingService.getEmbedding(query);
        log.info("Query embedding generated, dimensions: {}", queryVector.length);

        String url = String.format("http://%s:%d/collections/%s/points/search", qdrantHost, qdrantPort, collectionName);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("vector", toDoubleList(queryVector));
        requestBody.put("limit", topK);
        requestBody.put("with_payload", true);
        requestBody.put("with_vector", false);

        List<Map<String, Object>> mustFilters = new ArrayList<>();
        if (category != null && !category.isEmpty()) {
            mustFilters.add(Map.of("key", "category", "match", Map.of("value", category)));
        }
        if (person != null && !person.isEmpty()) {
            mustFilters.add(Map.of("key", "person", "match", Map.of("value", person)));
        }
        if (from != null) {
            mustFilters.add(Map.of("key", "date", "range", Map.of("gte", from.toString())));
        }
        if (to != null) {
            mustFilters.add(Map.of("key", "date", "range", Map.of("lte", to.toString())));
        }

        if (!mustFilters.isEmpty()) {
            requestBody.put("filter", Map.of("must", mustFilters));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode points = root.path("result");

            List<com.proiect.dto.EmbeddedExpense> results = new ArrayList<>();
            for (JsonNode point : points) {
                JsonNode payload = point.path("payload");
                double score = point.path("score").asDouble(0.0);

                com.proiect.dto.EmbeddedExpense embeddedExpense = com.proiect.dto.EmbeddedExpense.builder()
                        .id(parseLongFromPayload(payload.path("id")))
                        .amount(parseAmountFromPayload(payload.path("amount")))
                        .category(payload.path("category").asText(null))
                        .person(payload.path("person").asText(null))
                        .date(parseDateFromPayload(payload.path("date")))
                        .rawInput(payload.path("text_segment").asText(null))
                        .score(score)
                        .build();

                results.add(embeddedExpense);
            }

            log.info("Search returned {} results", results.size());
            return results;
        } catch (Exception e) {
            log.error("Error searching Qdrant vector store", e);
            throw new RuntimeException("Eroare la căutarea în vector store", e);
        }
    }

    public boolean existsInVectorStore(Long id) {
        try {
            String url = String.format("http://%s:%d/collections/%s/points/scroll", qdrantHost, qdrantPort, collectionName);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("limit", 1);
            requestBody.put("with_payload", false);
            requestBody.put("filter", Map.of("must", List.of(Map.of("key", "id", "match", Map.of("value", String.valueOf(id))))));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode points = root.path("result").path("points");
                return points.isArray() && points.size() > 0;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private List<Double> toDoubleList(float[] vector) {
        List<Double> list = new ArrayList<>(vector.length);
        for (float v : vector) {
            list.add((double) v);
        }
        return list;
    }

    private Long parseLongFromPayload(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        if (node.isNumber()) return node.asLong();
        try {
            return Long.parseLong(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseAmountFromPayload(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        if (node.isNumber()) return BigDecimal.valueOf(node.asDouble());
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDateFromPayload(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        try {
            return LocalDate.parse(node.asText());
        } catch (Exception e) {
            return null;
        }
    }
}
