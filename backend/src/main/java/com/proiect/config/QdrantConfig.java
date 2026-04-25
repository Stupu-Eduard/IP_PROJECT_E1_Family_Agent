package com.proiect.config;

import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class QdrantConfig {

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.port:6333}")
    private int httpPort;

    @Value("${qdrant.grpc-port:6334}")
    private int grpcPort;

    @Value("${qdrant.collection-name:expenses}")
    private String collectionName;

    private final RestTemplate restTemplate = new RestTemplate();

    @Bean
    public QdrantEmbeddingStore embeddingStore() {
        return QdrantEmbeddingStore.builder()
                .host(host)
                .port(grpcPort)
                .collectionName(collectionName)
                .build();
    }

    @PostConstruct
    public void initializeCollection() {
        String url = String.format("http://%s:%d/collections/%s", host, httpPort, collectionName);
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Qdrant collection '{}' already exists.", collectionName);
                return;
            }
        } catch (Exception e) {
            log.info("Qdrant collection '{}' not found, creating it...", collectionName);
        }

        // Create collection with 1536 dimensions and Cosine distance
        Map<String, Object> body = new HashMap<>();
        body.put("vectors", Map.of(
                "size", 1536,
                "distance", "Cosine"
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.put(url, entity);
            log.info("Successfully created Qdrant collection '{}' with 1536 dimensions and Cosine distance.", collectionName);
            
            // Create indexes for metadata
            createPayloadIndex("date", "keyword");
            createPayloadIndex("person", "keyword");
            createPayloadIndex("category", "keyword");
            createPayloadIndex("location", "keyword");
            
        } catch (Exception e) {
            log.error("Failed to create Qdrant collection: {}", e.getMessage());
        }
    }

    private void createPayloadIndex(String fieldName, String fieldType) {
        String url = String.format("http://%s:%d/collections/%s/index", host, httpPort, collectionName);
        Map<String, Object> body = new HashMap<>();
        body.put("field_name", fieldName);
        body.put("field_schema", fieldType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.put(url, entity);
            log.info("Created payload index for field: {}", fieldName);
        } catch (Exception e) {
            log.error("Failed to create payload index for field {}: {}", fieldName, e.getMessage());
        }
    }
}
