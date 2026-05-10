package com.proiect.service;
import org.springframework.test.context.ContextConfiguration;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private EmbeddingService embeddingService;

    @Test
    void testGetEmbedding() {
        float[] expectedVector = new float[]{0.1f, 0.2f, 0.3f};
        Embedding embedding = Embedding.from(expectedVector);
        when(embeddingModel.embed("test text")).thenReturn(Response.from(embedding));

        float[] result = embeddingService.getEmbedding("test text");

        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(0.1f, result[0]);
        assertEquals(0.2f, result[1]);
        assertEquals(0.3f, result[2]);
        verify(embeddingModel, times(1)).embed("test text");
    }
}
