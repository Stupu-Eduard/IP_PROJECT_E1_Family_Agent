package com.proiect.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proiect.dto.EmbeddedExpense;
import com.proiect.model.ExpenseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QdrantVectorServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore embeddingStore;

    @InjectMocks
    private QdrantVectorService qdrantVectorService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(qdrantVectorService, "qdrantHost", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "qdrantPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "expenses");
    }

    @Test
    void testStoreExpense() {
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .category("Food")
                .location("Kaufland")
                .person("Familie")
                .date(LocalDate.now())
                .rawInput("Am platit 100 lei la Kaufland")
                .build();

        float[] fakeVector = new float[2048];
        fakeVector[0] = 0.5f;
        when(embeddingService.getEmbedding("Am platit 100 lei la Kaufland")).thenReturn(fakeVector);

        qdrantVectorService.storeExpense(expense);

        verify(embeddingStore, times(1)).add(any(dev.langchain4j.data.embedding.Embedding.class), any(dev.langchain4j.data.segment.TextSegment.class));
        verify(embeddingService, times(1)).getEmbedding("Am platit 100 lei la Kaufland");
    }

    @Test
    void testStoreExpenseWithFallbackText() {
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(2L)
                .amount(new BigDecimal("50.00"))
                .category("Transport")
                .build();

        float[] fakeVector = new float[2048];
        when(embeddingService.getEmbedding("Transport")).thenReturn(fakeVector);

        qdrantVectorService.storeExpense(expense);

        verify(embeddingStore, times(1)).add(any(dev.langchain4j.data.embedding.Embedding.class), any(dev.langchain4j.data.segment.TextSegment.class));
        verify(embeddingService, times(1)).getEmbedding("Transport");
    }

    @Test
    void testSearchSimilar() {
        float[] fakeVector = new float[2048];
        fakeVector[0] = 0.3f;
        when(embeddingService.getEmbedding("mancare")).thenReturn(fakeVector);

        String jsonResponse = """
                {"result": [{"id": "abc", "score": 0.85, "payload": {"id": "1", "amount": "100.0", "category": "Food", "person": "Familie", "date": "2024-03-15", "text_segment": "Am platit 100 lei"}}], "status": "ok"}
                """;
        ResponseEntity<String> response = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        List<EmbeddedExpense> results = qdrantVectorService.searchSimilar("mancare", 5);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals(new BigDecimal("100.0"), results.get(0).getAmount());
        assertEquals("Food", results.get(0).getCategory());
        assertEquals(0.85, results.get(0).getScore(), 0.001);
    }

    @Test
    void testSearchWithFilter() {
        float[] fakeVector = new float[2048];
        when(embeddingService.getEmbedding("benzina")).thenReturn(fakeVector);

        String jsonResponse = """
                {"result": [{"id": "def", "score": 0.92, "payload": {"id": "2", "amount": "200.0", "category": "Transport", "person": "Teodor", "date": "2024-03-10", "text_segment": "Am platit 200 lei la benzinărie"}}], "status": "ok"}
                """;
        ResponseEntity<String> response = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                "benzina", 5, "Transport", "Teodor", LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31));

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getId());
        assertEquals("Transport", results.get(0).getCategory());
    }

    @Test
    void testSearchWithEmptyResults() {
        float[] fakeVector = new float[2048];
        when(embeddingService.getEmbedding("vacanta")).thenReturn(fakeVector);

        String jsonResponse = "{\"result\": [], \"status\": \"ok\"}";
        ResponseEntity<String> response = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        List<EmbeddedExpense> results = qdrantVectorService.searchSimilar("vacanta", 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testExistsInVectorStoreReturnsTrue() {
        String jsonResponse = "{\"result\": {\"points\": [{\"id\": \"abc\"}]}, \"status\": \"ok\"}";
        ResponseEntity<String> response = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        assertTrue(qdrantVectorService.existsInVectorStore(1L));
    }

    @Test
    void testExistsInVectorStoreReturnsFalse() {
        String jsonResponse = "{\"result\": {\"points\": []}, \"status\": \"ok\"}";
        ResponseEntity<String> response = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        assertFalse(qdrantVectorService.existsInVectorStore(99L));
    }

    @Test
    void testExistsInVectorStoreHandlesException() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertFalse(qdrantVectorService.existsInVectorStore(1L));
    }

    @Test
    void testSearchHandlesQdrantError() {
        float[] fakeVector = new float[2048];
        when(embeddingService.getEmbedding("query")).thenReturn(fakeVector);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Qdrant is down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> qdrantVectorService.searchSimilar("query", 5));
        assertTrue(ex.getMessage().contains("Eroare la căutarea în vector store"));
    }
}
