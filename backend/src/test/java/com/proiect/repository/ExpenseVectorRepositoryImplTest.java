package com.proiect.repository;

import com.proiect.model.ExpenseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ExpenseVectorRepositoryImplTest {

    private ExpenseVectorRepositoryImpl repository;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() throws Exception {
        repository = new ExpenseVectorRepositoryImpl();

        // Replace the internal RestTemplate with a mockable one via reflection
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        java.lang.reflect.Field field = ExpenseVectorRepositoryImpl.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        field.set(repository, restTemplate);
    }

    @Test
    void testSaveVector_Success() {
        ExpenseEntity entity = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .category("Food")
                .build();

        mockServer.expect(requestTo("http://localhost:6333/collections/expenses/points?wait=true"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{\"status\": \"ok\"}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> repository.saveVector(entity, new float[]{0.1f, 0.2f}));
        mockServer.verify();
    }

    @Test
    void testSaveVector_NullId_ThrowsException() {
        ExpenseEntity entity = ExpenseEntity.builder()
                .amount(new BigDecimal("100.00"))
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> repository.saveVector(entity, new float[]{0.1f}));
        assertTrue(ex.getMessage().contains("entity has no ID yet"));
    }

    @Test
    void testSaveVector_QdrantError_ThrowsException() {
        ExpenseEntity entity = ExpenseEntity.builder()
                .id(2L)
                .amount(new BigDecimal("50.00"))
                .build();

        mockServer.expect(requestTo("http://localhost:6333/collections/expenses/points?wait=true"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withServerError());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> repository.saveVector(entity, new float[]{0.1f}));
        assertTrue(ex.getMessage().contains("Failed to save vector to Qdrant"));
    }

    @Test
    void testExistsInVectorStore_True() {
        mockServer.expect(requestTo("http://localhost:6333/collections/expenses/points/1"))
                .andRespond(withSuccess("{\"result\": {}}", MediaType.APPLICATION_JSON));

        assertTrue(repository.existsInVectorStore(1L));
        mockServer.verify();
    }

    @Test
    void testExistsInVectorStore_NotFound() {
        mockServer.expect(requestTo("http://localhost:6333/collections/expenses/points/99"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertFalse(repository.existsInVectorStore(99L));
        mockServer.verify();
    }

    @Test
    void testExistsInVectorStore_GenericError() {
        mockServer.expect(requestTo("http://localhost:6333/collections/expenses/points/3"))
                .andRespond(withServerError());

        assertFalse(repository.existsInVectorStore(3L));
        mockServer.verify();
    }
}
