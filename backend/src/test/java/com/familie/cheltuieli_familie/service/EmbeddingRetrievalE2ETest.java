package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.model.Expense;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "OPENROUTER_API_KEY=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",
    "qdrant.host=localhost",
    "qdrant.port=6333",
    "qdrant.collection-name=e2e-test-expenses"
})
class EmbeddingRetrievalE2ETest {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private QdrantVectorService qdrantVectorService;

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.port:6333}")
    private int httpPort;

    @Value("${qdrant.collection-name:e2e-test-expenses}")
    private String collectionName;

    private final RestTemplate restTemplate = new RestTemplate();

    @BeforeEach
    void setUp() {
        // Skip if Qdrant is not reachable
        try {
            java.net.Socket socket = new java.net.Socket("localhost", 6333);
            socket.close();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Qdrant not available on localhost:6333");
            return;
        }

        // Clear all points from the test collection before each test
        clearCollection();
    }

    private void clearCollection() {
        String url = String.format("http://%s:%d/collections/%s/points/delete", host, httpPort, collectionName);
        Map<String, Object> body = Map.of("filter", Map.of());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            // Ignore errors (collection might not exist yet)
        }
    }

    @Test
    void testEmbeddingGeneration() {
        String text = "Am cheltuit 150 lei la supermarket pentru mâncare";
        float[] embedding = embeddingService.getEmbedding(text);

        assertNotNull(embedding);
        assertEquals(2048, embedding.length, "Embedding dimension should be 2048 for nvidia/llama-nemotron-embed-vl-1b-v2");
    }

    @Test
    void testStoreAndRetrieveExpense() {
        long uniqueId = System.currentTimeMillis();
        Expense expense = Expense.builder()
                .id(uniqueId)
                .amount(new BigDecimal("150.00"))
                .aiCategory("Mâncare")
                .aiPerson("Alice")
                .aiLocation("Kaufland")
                .expenseDate(LocalDateTime.of(2024, 3, 15, 0, 0))
                .rawInput("Am cumpărat 150 lei de mâncare de la Kaufland")
                .build();

        qdrantVectorService.storeExpense(expense);

        List<EmbeddedExpense> results = Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> qdrantVectorService.searchSimilar("mâncare Kaufland", 5), r -> !r.isEmpty());

        assertFalse(results.isEmpty(), "Should find the stored expense");
        assertTrue(results.stream().anyMatch(r -> r.getId() != null && r.getId() == uniqueId),
                "Should find the exact expense we stored");
    }

    @Test
    void testStoreAndSearchWithFilter() {
        long uniqueId = System.currentTimeMillis();
        Expense expense = Expense.builder()
                .id(uniqueId)
                .amount(new BigDecimal("200.00"))
                .aiCategory("Transport")
                .aiPerson("Bob")
                .aiLocation("Metro")
                .expenseDate(LocalDateTime.of(2024, 4, 10, 0, 0))
                .rawInput("Bilet metro 200 lei")
                .build();

        qdrantVectorService.storeExpense(expense);

        List<EmbeddedExpense> results = Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> qdrantVectorService.searchWithFilter(
                        "transport", 5, "Transport", "Bob", null, null), r -> !r.isEmpty());

        assertFalse(results.isEmpty(), "Should find the expense with filters");
    }

    @Test
    void testExistsInVectorStore() {
        long uniqueId = System.currentTimeMillis();
        Expense expense = Expense.builder()
                .id(uniqueId)
                .amount(new BigDecimal("50.00"))
                .aiCategory("Divertisment")
                .aiPerson("Charlie")
                .aiLocation("Cinema")
                .expenseDate(LocalDateTime.of(2024, 5, 1, 0, 0))
                .rawInput("Bilet cinema 50 lei")
                .build();

        qdrantVectorService.storeExpense(expense);

        boolean exists = Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> qdrantVectorService.existsInVectorStore(uniqueId), result -> result);
        assertTrue(exists, "Expense should exist in vector store");

        boolean notExists = qdrantVectorService.existsInVectorStore(uniqueId + 99999);
        assertFalse(notExists, "Random ID should not exist");
    }
}
