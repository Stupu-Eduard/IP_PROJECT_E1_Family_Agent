package com.familie.cheltuieli_familie.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.familie.cheltuieli_familie.model.ExpenseEntity;

import com.familie.cheltuieli_familie.exception.VectorStoreException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of ExpenseVectorRepository using Qdrant REST API.
 */
@Repository
public class ExpenseVectorRepositoryImpl implements ExpenseVectorRepository {

    private final RestTemplate restTemplate;
    private final String qdrantUrl;

    @Autowired
    public ExpenseVectorRepositoryImpl(RestTemplate restTemplate, 
                                     @Value("${qdrant.host:localhost}") String qdrantHost, 
                                     @Value("${qdrant.port:6333}") int qdrantPort) {
        this.restTemplate = restTemplate;
        this.qdrantUrl = String.format("http://%s:%d/collections/expenses/points", qdrantHost, qdrantPort);
    }

    @Override
    public void saveVector(ExpenseEntity entity, float[] vector) {
        String url = qdrantUrl + "?wait=true";

        if (entity.getId() == null) {
            throw new VectorStoreException("Cannot save vector: entity has no ID yet");
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

        String putUrl = qdrantUrl + "?wait=true";
        org.springframework.http.HttpEntity<Map<String, Object>> httpEntity =
            new org.springframework.http.HttpEntity<>(requestBody);
        try {
            restTemplate.exchange(putUrl, org.springframework.http.HttpMethod.PUT, httpEntity, String.class);
        } catch (Exception e) {
            throw new VectorStoreException("Failed to save vector to Qdrant: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsInVectorStore(Long id) {
        String url = qdrantUrl + "/" + id;
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
