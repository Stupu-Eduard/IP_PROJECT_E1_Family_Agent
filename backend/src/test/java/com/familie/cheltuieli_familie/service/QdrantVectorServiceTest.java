package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QdrantVectorServiceTest {

    @Mock
    private QdrantEmbeddingStore embeddingStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private QdrantVectorService qdrantVectorService;

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

        when(embeddingModel.embed(any(TextSegment.class))).thenReturn(Response.from(Embedding.from(new float[2048])));

        qdrantVectorService.storeExpense(expense);

        verify(embeddingStore, atLeastOnce()).add(any(Embedding.class), any(TextSegment.class));
        verify(embeddingModel, atLeastOnce()).embed(any(TextSegment.class));
    }

    @Test
    void testSearchWithFilter() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[2048])));
        
        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        Map<String, Object> mockResponse = Map.of("result", List.of(Map.of(
                "score", 0.95,
                "payload", Map.of(
                        "id", "1",
                        "amount", "100.0",
                        "category", "Food",
                        "person", "Familie",
                        "location", "Kaufland",
                        "date", LocalDate.now().toString(),
                        "text_segment", "Am platit 100 lei la Kaufland"
                )
        )));

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                "query", 5, "Food", "Familie", LocalDate.now(), LocalDate.now());

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Food", results.get(0).getCategory());
        verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    void testExistsInVectorStore() {
        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        Map<String, Object> mockResponse = Map.of("result", List.of(Map.of(
                "score", 0.95,
                "payload", Map.of("id", "1")
        )));

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        boolean exists = qdrantVectorService.existsInVectorStore(1L);

        assertTrue(exists);
        verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    void testExistsInVectorStoreNotFound() {
        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        Map<String, Object> mockResponse = Map.of("result", List.of());

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        boolean exists = qdrantVectorService.existsInVectorStore(999L);

        assertFalse(exists);
    }
}
