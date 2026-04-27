package com.familie.cheltuieli_familie.repository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.familie.cheltuieli_familie.model.ExpenseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of ExpenseVectorRepository using Qdrant REST API.
 */
@Repository
public class ExpenseVectorRepositoryImpl implements ExpenseVectorRepository {

    private final RestTemplate restTemplate;
    private static final String QDRANT_URL = "http://localhost:6333/collections/expenses/points";

    public ExpenseVectorRepositoryImpl() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public void saveVector(ExpenseEntity entity, float[] vector) {
        String url = QDRANT_URL + "?wait=true";

        if (entity.getId() == null) {
            throw new RuntimeException("Cannot save vector: entity has no ID yet");
        }

        // Qdrant REST upsert - PUT /collections/{name}/points
        // Each point needs id, vector, and optional payload
        Map<String, Object> point = new HashMap<>();
        point.put("id", entity.getId());            // numeric id
        point.put("vector", vector);                // float array

        Map<String, Object> payload = new HashMap<>();
        payload.put("entityId", entity.getId());
        point.put("payload", payload);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("points", Collections.singletonList(point));

        String putUrl = "http://localhost:6333/collections/expenses/points?wait=true";
        org.springframework.http.HttpEntity<Map<String, Object>> httpEntity =
            new org.springframework.http.HttpEntity<>(requestBody);
        try {
            restTemplate.exchange(putUrl, org.springframework.http.HttpMethod.PUT, httpEntity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save vector to Qdrant: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsInVectorStore(Long id) {
        String url = QDRANT_URL + "/" + id;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
