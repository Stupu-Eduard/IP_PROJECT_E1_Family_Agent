package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.response.ChartResponseDTO;
import com.familie.cheltuieli_familie.model.ChartFilters;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import com.familie.cheltuieli_familie.model.ChartQueryResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChartGenerationServiceTest {

    @Mock
    private SemanticExpansionService semanticExpansionService;

    @Mock
    private ChartQueryExecutor chartQueryExecutor;

    @InjectMocks
    private ChartGenerationService chartGenerationService;

    @Test
    void generate_shouldReturnResponse_withEmptyData_whenNoRowsFound() {
        ChartQueryIntent intent = new ChartQueryIntent();
        intent.setTitle("Test Chart");
        
        when(chartQueryExecutor.execute(any(), any(), any()))
                .thenReturn(new ChartQueryResult(Collections.emptyList(), List.of("total"), "name"));

        ChartResponseDTO response = chartGenerationService.generate(intent);

        assertEquals("Nu am găsit cheltuieli pentru criteriile selectate.", response.getMessage());
        assertNotNull(response.getPayload());
        assertTrue(response.getPayload().getData().isEmpty());
    }

    @Test
    void generate_shouldReturnResponse_withSingleSeriesExplanation() {
        ChartQueryIntent intent = new ChartQueryIntent();
        intent.setTitle("Monthly Spending");
        ChartFilters filters = new ChartFilters();
        filters.setCategory("Food");
        intent.setFilters(filters);

        when(semanticExpansionService.expandCategories("Food")).thenReturn(List.of("Food", "Groceries"));

        List<Map<String, Object>> rows = List.of(
                Map.of("name", "Jan", "total", new BigDecimal("100")),
                Map.of("name", "Feb", "total", new BigDecimal("150"))
        );
        ChartQueryResult result = new ChartQueryResult(rows, List.of("total"), "name");
        when(chartQueryExecutor.execute(eq(intent), any(), any())).thenReturn(result);

        ChartResponseDTO response = chartGenerationService.generate(intent);

        assertTrue(response.getMessage().contains("Jan: 100 RON"));
        assertTrue(response.getMessage().contains("Feb: 150 RON"));
        assertEquals("Monthly Spending", response.getPayload().getTitle());
    }

    @Test
    void generate_shouldReturnResponse_withMultiSeriesExplanation() {
        ChartQueryIntent intent = new ChartQueryIntent();
        intent.setTitle("Member Comparison");

        List<Map<String, Object>> rows = List.of(
                Map.of("name", "Jan", "Dad", new BigDecimal("50"), "Mom", new BigDecimal("60")),
                Map.of("name", "Feb", "Dad", new BigDecimal("40"), "Mom", new BigDecimal("70"))
        );
        ChartQueryResult result = new ChartQueryResult(rows, List.of("Dad", "Mom"), "name");
        when(chartQueryExecutor.execute(any(), any(), any())).thenReturn(result);

        ChartResponseDTO response = chartGenerationService.generate(intent);

        assertTrue(response.getMessage().contains("Dad: 90 RON total"));
        assertTrue(response.getMessage().contains("Mom: 130 RON total"));
    }
}
