package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.exception.VectorStoreException;
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
import org.springframework.core.ParameterizedTypeReference;
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
    void testStoreExpenseWithNullRawInput() {
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(2L)
                .amount(new BigDecimal("50.00"))
                .category("Transport")
                .location("Metrorex")
                .person("Ion")
                .date(LocalDate.of(2024, 1, 15))
                .rawInput(null)
                .build();

        when(embeddingModel.embed(any(TextSegment.class))).thenReturn(Response.from(Embedding.from(new float[2048])));

        qdrantVectorService.storeExpense(expense);

        verify(embeddingStore, atLeastOnce()).add(any(Embedding.class), any(TextSegment.class));
        verify(embeddingModel, atLeastOnce()).embed(any(TextSegment.class));
    }

    @Test
    void testStoreExpenseWithEmptyRawInput() {
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(3L)
                .amount(new BigDecimal("75.00"))
                .category("Utilities")
                .location("Enel")
                .person("Maria")
                .date(LocalDate.of(2024, 2, 20))
                .rawInput("")
                .build();

        when(embeddingModel.embed(any(TextSegment.class))).thenReturn(Response.from(Embedding.from(new float[2048])));

        qdrantVectorService.storeExpense(expense);

        verify(embeddingStore, atLeastOnce()).add(any(Embedding.class), any(TextSegment.class));
    }

    @Test
    void testStoreExpenseThrowsVectorStoreException() {
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(4L)
                .amount(new BigDecimal("100.00"))
                .category("Food")
                .location("Kaufland")
                .person("Familie")
                .date(LocalDate.now())
                .rawInput("Am platit 100 lei la Kaufland")
                .build();

        when(embeddingModel.embed(any(TextSegment.class))).thenThrow(new RuntimeException("Embedding failed"));

        VectorStoreException ex = assertThrows(VectorStoreException.class, () -> qdrantVectorService.storeExpense(expense));
        assertTrue(ex.getMessage().contains("Embedding failed for expense 4"));
    }

    @Test
    void testSearchSimilar() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[2048])));

        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        Map<String, Object> mockResponse = Map.of("result", List.of(Map.of(
                "score", 0.95,
                "payload", Map.of(
                        "expense_id", "1",
                        "amount", "100.0",
                        "category", "Food",
                        "person", "Familie",
                        "location", "Kaufland",
                        "date", LocalDate.now().toString(),
                        "text_segment", "Am platit 100 lei la Kaufland"
                )
        )));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<EmbeddedExpense> results = qdrantVectorService.searchSimilar("query", 5);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Food", results.get(0).getCategory());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class));
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
                        "expense_id", "1",
                        "amount", "100.0",
                        "category", "Food",
                        "person", "Familie",
                        "location", "Kaufland",
                        "date", LocalDate.now().toString(),
                        "text_segment", "Am platit 100 lei la Kaufland"
                )
        )));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                "query", 5, "Food", "Familie", LocalDate.now(), LocalDate.now());

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Food", results.get(0).getCategory());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    void testExistsInVectorStore() {
        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        Map<String, Object> mockResponse = Map.of("result", Map.of(
                "points", List.of(Map.of("id", "some-uuid"))
        ));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        boolean exists = qdrantVectorService.existsInVectorStore(1L);

        assertTrue(exists);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    void testExistsInVectorStoreNotFound() {
        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(Map.of("result", Map.of("points", List.of())), HttpStatus.OK));

        boolean exists = qdrantVectorService.existsInVectorStore(999L);

        assertFalse(exists);
    }

    @Test
    void testSearchWithNoFilters() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[2048])));

        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        Map<String, Object> mockResponse = Map.of("result", List.of(Map.of(
                "score", 0.95,
                "payload", Map.of(
                        "expense_id", 1,
                        "amount", 100.0,
                        "category", "Food"
                )
        )));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                "query", 5, null, null, null, null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Food", results.get(0).getCategory());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    void testSearchWithSingleFilter() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[2048])));

        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        Map<String, Object> mockResponse = Map.of("result", List.of(Map.of(
                "score", 0.95,
                "payload", Map.of(
                        "expense_id", 1,
                        "amount", 100.0,
                        "category", "Food"
                )
        )));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                "query", 5, "Food", null, null, null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Food", results.get(0).getCategory());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    void testSearchWithDateRangeFilter() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[2048])));

        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        Map<String, Object> mockResponse = Map.of("result", List.of(Map.of(
                "score", 0.92,
                "payload", Map.of(
                        "expense_id", 5,
                        "amount", 200.0,
                        "category", "Utilities",
                        "date", "2024-03-15"
                )
        )));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                "query", 5, null, null, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Utilities", results.get(0).getCategory());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    void testExistsInVectorStoreWithException() {
        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Qdrant error"));

        boolean exists = qdrantVectorService.existsInVectorStore(1L);

        assertFalse(exists);
    }

    @Test
    void testExecuteQdrantSearchWithNonOkStatus() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[2048])));

        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(Map.of("result", List.of()), HttpStatus.BAD_REQUEST));

        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                "query", 5, null, null, null, null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testExecuteQdrantSearchWithException() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[2048])));

        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                "query", 5, null, null, null, null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testMapRestResultWithStringIdAndAmount() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[2048])));

        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        Map<String, Object> mockResponse = Map.of("result", List.of(Map.of(
                "score", 0.85,
                "payload", Map.of(
                        "expense_id", "42",
                        "amount", "99.50",
                        "category", "Food",
                        "person", "Familie",
                        "location", "Kaufland",
                        "date", LocalDate.now().toString(),
                        "text_segment", "Test"
                )
        )));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                "query", 5, "Food", null, null, null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(42L, results.get(0).getId());
        assertEquals(new BigDecimal("99.50"), results.get(0).getAmount());
    }

    @Test
    void testMapRestResultWithNullPayload() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[2048])));

        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        Map<String, Object> mockResponse = Map.of("result", List.of(Map.of(
                "score", 0.75
        )));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                "query", 5, null, null, null, null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertNull(results.get(0).getId());
        assertNull(results.get(0).getAmount());
        assertNull(results.get(0).getCategory());
        assertEquals(0.75, results.get(0).getScore(), 0.001);
    }

    @Test
    void testParseLocalDateWithInvalidDate() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[2048])));

        ReflectionTestUtils.setField(qdrantVectorService, "host", "localhost");
        ReflectionTestUtils.setField(qdrantVectorService, "httpPort", 6333);
        ReflectionTestUtils.setField(qdrantVectorService, "collectionName", "test-collection");

        Map<String, Object> mockResponse = Map.of("result", List.of(Map.of(
                "score", 0.80,
                "payload", Map.of(
                        "expense_id", 1,
                        "amount", 100.0,
                        "category", "Food",
                        "date", "invalid-date"
                )
        )));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                "query", 5, null, null, null, null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertNull(results.get(0).getDate());
    }
}
