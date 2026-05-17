package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticExpansionServiceTest {

    @Mock
    private QdrantVectorService qdrantVectorService;

    @InjectMocks
    private SemanticExpansionService semanticExpansionService;

    @Test
    void expandCategories_shouldReturnEmpty_whenInputNullOrBlank() {
        assertTrue(semanticExpansionService.expandCategories(null).isEmpty());
        assertTrue(semanticExpansionService.expandCategories("  ").isEmpty());
    }

    @Test
    void expandCategories_shouldReturnResults_whenSearchSucceeds() {
        EmbeddedExpense e1 = new EmbeddedExpense();
        e1.setCategory("Food");
        e1.setScore(0.8);

        EmbeddedExpense e2 = new EmbeddedExpense();
        e2.setCategory("Groceries");
        e2.setScore(0.4); // Below threshold

        when(qdrantVectorService.searchSimilar(anyString(), anyInt()))
                .thenReturn(List.of(e1, e2));

        List<String> result = semanticExpansionService.expandCategories("something fuzzy");
        
        assertEquals(1, result.size());
        assertEquals("Food", result.get(0));
    }

    @Test
    void expandCategories_shouldFallback_whenExceptionOccurs() {
        when(qdrantVectorService.searchSimilar(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Qdrant error"));

        List<String> result = semanticExpansionService.expandCategories("fuzzy");
        
        assertEquals(1, result.size());
        assertEquals("fuzzy", result.get(0));
    }

    @Test
    void expandLocations_shouldReturnResults_whenSearchSucceeds() {
        EmbeddedExpense e1 = new EmbeddedExpense();
        e1.setLocation("Mega Image");
        e1.setScore(0.9);

        when(qdrantVectorService.searchSimilar(anyString(), anyInt()))
                .thenReturn(List.of(e1));

        List<String> result = semanticExpansionService.expandLocations("market");
        
        assertEquals(1, result.size());
        assertEquals("Mega Image", result.get(0));
    }

    @Test
    void expandLocations_shouldFallback_whenExceptionOccurs() {
        when(qdrantVectorService.searchSimilar(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Qdrant error"));

        List<String> result = semanticExpansionService.expandLocations("market");
        
        assertEquals(1, result.size());
        assertEquals("market", result.get(0));
    }
}
