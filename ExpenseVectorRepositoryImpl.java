package com.proiect.repository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface for managing vector-based storage of expenses.
 */
public interface ExpenseVectorRepository {
    /**
     * Saves a vector representation of an expense to Qdrant.
     * @param id The ID of the expense.
     * @param vector The vector to save.
     */
    void saveVector(Long id, float[] vector);

    /**
     * Checks if a vector exists for the given ID in Qdrant.
     * @param id The ID to check.
     * @return true if it exists (HTTP 200), false otherwise.
     */
    boolean existsInVectorStore(Long id);
}

/**
 * Implementation of ExpenseVectorRepository using Qdrant REST API.
 */
@Repository
public class ExpenseVectorRepositoryImpl implements ExpenseVectorRepository {

    private final RestTemplate restTemplate;
    private static final String QDRANT_URL = "http://localhost:6333/collections/expenses/points";

    public ExpenseVectorRepositoryImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void saveVector(Long id, float[] vector) {
        String url = QDRANT_URL + "?wait=true";

        // Prepare the point data
        Map<String, Object> point = new HashMap<>();
        point.put("id", id);
        point.put("vector", vector);
        
        // Payload with amount and category (using placeholders as Entity isn't passed here directly)
        Map<String, Object> payload = new HashMap<>();
        // In a real scenario, these would be passed from the service
        payload.put("info", "Sync for expense " + id); 
        point.put("payload", payload);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("points", Collections.singletonList(point));

        try {
            restTemplate.postForEntity(url, requestBody, String.class);
        } catch (Exception e) {
            // Log error or handle appropriately in production
            throw new RuntimeException("Failed to save vector to Qdrant", e);
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
