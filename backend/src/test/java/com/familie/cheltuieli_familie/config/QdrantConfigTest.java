package com.familie.cheltuieli_familie.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QdrantConfigTest {

    @Test
    void testEmbeddingStoreBean() {
        QdrantConfig config = new QdrantConfig();
        ReflectionTestUtils.setField(config, "collectionName", "test-collection");
        ReflectionTestUtils.setField(config, "grpcPort", 6334);
        ReflectionTestUtils.setField(config, "host", "localhost");
        var store = config.embeddingStore();
        assertNotNull(store);
    }

    @Test
    void testInitializeCollectionExists() {
        QdrantConfig config = new QdrantConfig();
        ReflectionTestUtils.setField(config, "collectionName", "test-collection");
        ReflectionTestUtils.setField(config, "httpPort", 6333);
        ReflectionTestUtils.setField(config, "host", "localhost");
        ReflectionTestUtils.setField(config, "vectorSize", 2048);
        ReflectionTestUtils.setField(config, "distance", "Cosine");
        ReflectionTestUtils.setField(config, "indexType", "keyword");
        
        RestTemplate mockRest = mock(RestTemplate.class);
        ResponseEntity<String> response = new ResponseEntity<>("exists", HttpStatus.OK);
        when(mockRest.getForEntity(anyString(), eq(String.class))).thenReturn(response);

        ReflectionTestUtils.setField(config, "restTemplate", mockRest);
        
        config.initializeCollection();
        
        verify(mockRest).getForEntity(anyString(), eq(String.class));
    }

    @Test
    void testInitializeCollectionCreatesNew() {
        QdrantConfig config = new QdrantConfig();
        ReflectionTestUtils.setField(config, "collectionName", "test-collection");
        ReflectionTestUtils.setField(config, "httpPort", 6333);
        ReflectionTestUtils.setField(config, "host", "localhost");
        ReflectionTestUtils.setField(config, "vectorSize", 2048);
        ReflectionTestUtils.setField(config, "distance", "Cosine");
        ReflectionTestUtils.setField(config, "indexType", "keyword");
        
        RestTemplate mockRest = mock(RestTemplate.class);
        when(mockRest.getForEntity(anyString(), eq(String.class))).thenThrow(new RuntimeException("Not found"));

        ReflectionTestUtils.setField(config, "restTemplate", mockRest);
        
        config.initializeCollection();
        
        verify(mockRest, times(5)).put(anyString(), any());
    }
}
