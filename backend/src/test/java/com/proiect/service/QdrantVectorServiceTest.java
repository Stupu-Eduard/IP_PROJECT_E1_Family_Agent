package com.proiect.service;

import com.proiect.dto.EmbeddedExpense;
import com.proiect.model.ExpenseEntity;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.springframework.test.context.ActiveProfiles("test")
class QdrantVectorServiceTest {

    @Mock
    private QdrantEmbeddingStore embeddingStore;

    @Mock
    private EmbeddingModel embeddingModel;

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

        when(embeddingModel.embed(any(TextSegment.class))).thenReturn(Response.from(Embedding.from(new float[1536])));

        qdrantVectorService.storeExpense(expense);

        verify(embeddingStore, atLeastOnce()).add(any(Embedding.class), any(TextSegment.class));
        verify(embeddingModel, atLeastOnce()).embed(any(TextSegment.class));
    }

    @Test
    void testSearchWithFilter() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[1536])));
        
        EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(Collections.emptyList());
        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(searchResult);

        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                "query", 5, "Food", "Familie", LocalDate.now(), LocalDate.now());

        assertNotNull(results);
        verify(embeddingStore).search(any(EmbeddingSearchRequest.class));
    }

    @Test
    void testExistsInVectorStore() {
        EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(Collections.emptyList());
        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(searchResult);

        boolean exists = qdrantVectorService.existsInVectorStore(1L);

        assertFalse(exists);
        verify(embeddingStore).search(any(EmbeddingSearchRequest.class));
    }
}
