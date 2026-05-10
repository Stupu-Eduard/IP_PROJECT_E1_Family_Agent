package com.proiect.service;
import org.springframework.test.context.ContextConfiguration;

import com.proiect.dto.EmbeddedExpense;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticExpansionServiceTest {

    @Mock
    private QdrantVectorService qdrantVectorService;

    @InjectMocks
    private SemanticExpansionService semanticExpansionService;

    @Test
    void testExpandCategories() {
        EmbeddedExpense e1 = EmbeddedExpense.builder().category("shopping").build();
        EmbeddedExpense e2 = EmbeddedExpense.builder().category("haine").build();
        EmbeddedExpense e3 = EmbeddedExpense.builder().category("shopping").build();

        when(qdrantVectorService.searchSimilar("mall shopping", 20))
                .thenReturn(List.of(e1, e2, e3));

        List<String> result = semanticExpansionService.expandCategories("mall shopping");

        assertEquals(List.of("shopping", "haine"), result);
    }

    @Test
    void testExpandCategoriesEmptyInput() {
        List<String> result = semanticExpansionService.expandCategories(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testExpandCategoriesFallbackOnError() {
        when(qdrantVectorService.searchSimilar("test", 20))
                .thenThrow(new RuntimeException("Qdrant error"));

        List<String> result = semanticExpansionService.expandCategories("test");

        assertEquals(List.of("test"), result);
    }

    @Test
    void testExpandLocations() {
        EmbeddedExpense e1 = EmbeddedExpense.builder().location("Bucuresti").build();
        EmbeddedExpense e2 = EmbeddedExpense.builder().location("Cluj").build();

        when(qdrantVectorService.searchSimilar("orașe mari", 20))
                .thenReturn(List.of(e1, e2));

        List<String> result = semanticExpansionService.expandLocations("orașe mari");

        assertEquals(List.of("Bucuresti", "Cluj"), result);
    }
}
