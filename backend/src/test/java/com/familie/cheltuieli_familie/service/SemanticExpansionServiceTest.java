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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticExpansionServiceTest {

    @Mock
    private QdrantVectorService qdrantVectorService;

    @InjectMocks
    private SemanticExpansionService semanticExpansionService;

    @Test
    void expandCategories_shouldReturnEmptyListForNullInput() {
        List<String> result = semanticExpansionService.expandCategories(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void expandCategories_shouldReturnEmptyListForBlankInput() {
        List<String> result = semanticExpansionService.expandCategories("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    void expandCategories_shouldReturnExpandedCategories() {
        String fuzzy = "mall shopping";
        List<EmbeddedExpense> mockResults = List.of(
                EmbeddedExpense.builder().category("shopping").build(),
                EmbeddedExpense.builder().category("haine").build(),
                EmbeddedExpense.builder().category("shopping").build(),
                EmbeddedExpense.builder().category(null).build(),
                EmbeddedExpense.builder().category("").build()
        );

        when(qdrantVectorService.searchSimilar(fuzzy, 20)).thenReturn(mockResults);

        List<String> result = semanticExpansionService.expandCategories(fuzzy);

        assertEquals(List.of("shopping", "haine"), result);
        verify(qdrantVectorService).searchSimilar(fuzzy, 20);
    }

    @Test
    void expandCategories_shouldFallbackOnException() {
        String fuzzy = "food";
        when(qdrantVectorService.searchSimilar(fuzzy, 20)).thenThrow(new RuntimeException("Qdrant down"));

        List<String> result = semanticExpansionService.expandCategories(fuzzy);

        assertEquals(List.of(fuzzy), result);
    }

    @Test
    void expandLocations_shouldReturnEmptyListForNullInput() {
        List<String> result = semanticExpansionService.expandLocations(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void expandLocations_shouldReturnEmptyListForBlankInput() {
        List<String> result = semanticExpansionService.expandLocations("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    void expandLocations_shouldReturnExpandedLocations() {
        String fuzzy = "mall";
        List<EmbeddedExpense> mockResults = List.of(
                EmbeddedExpense.builder().location("Afi Cotroceni").build(),
                EmbeddedExpense.builder().location("Baneasa Shopping City").build(),
                EmbeddedExpense.builder().location("Afi Cotroceni").build(),
                EmbeddedExpense.builder().location(null).build()
        );

        when(qdrantVectorService.searchSimilar(fuzzy, 20)).thenReturn(mockResults);

        List<String> result = semanticExpansionService.expandLocations(fuzzy);

        assertEquals(List.of("Afi Cotroceni", "Baneasa Shopping City"), result);
    }

    @Test
    void expandLocations_shouldFallbackOnException() {
        String fuzzy = "Bucuresti";
        when(qdrantVectorService.searchSimilar(fuzzy, 20)).thenThrow(new RuntimeException("Qdrant down"));

        List<String> result = semanticExpansionService.expandLocations(fuzzy);

        assertEquals(List.of(fuzzy), result);
    }

    @Test
    void expandCategories_shouldReturnEmptyWhenNoResults() {
        String fuzzy = "unknown";
        when(qdrantVectorService.searchSimilar(fuzzy, 20)).thenReturn(Collections.emptyList());

        List<String> result = semanticExpansionService.expandCategories(fuzzy);

        assertTrue(result.isEmpty());
    }
}
