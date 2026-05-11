package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.response.ChartPayload;
import com.familie.cheltuieli_familie.dto.response.ChartResponseDTO;
import com.familie.cheltuieli_familie.model.ChartFilters;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import com.familie.cheltuieli_familie.model.ChartQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChartGenerationServiceTest {

    @Mock
    private SemanticExpansionService semanticExpansionService;

    @Mock
    private ChartQueryExecutor chartQueryExecutor;

    @InjectMocks
    private ChartGenerationService chartGenerationService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void generate_shouldReturnChartResponse_withSingleSeries() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .chartType("bar")
                .title("Expenses by Category")
                .aggregation("sum")
                .groupBy("category")
                .filters(ChartFilters.builder()
                        .category("food")
                        .build())
                .build();

        when(semanticExpansionService.expandCategories("food")).thenReturn(List.of("food", "mancare"));
        when(chartQueryExecutor.execute(intent, List.of("food", "mancare"), null))
                .thenReturn(ChartQueryResult.builder()
                        .rows(List.of(
                                Map.of("name", "food", "value", new BigDecimal("100")),
                                Map.of("name", "mancare", "value", new BigDecimal("200"))
                        ))
                        .seriesNames(List.of("value"))
                        .labelKey("name")
                        .build());

        ChartResponseDTO result = chartGenerationService.generate(intent);

        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertEquals("bar", result.getPayload().getChartType());
        assertEquals("Expenses by Category", result.getPayload().getTitle());
        assertEquals(2, result.getPayload().getData().size());
    }

    @Test
    void generate_shouldReturnChartResponse_withMultiSeries() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .chartType("line")
                .title("Expenses by Month and Person")
                .aggregation("sum")
                .groupBy("month")
                .seriesBy("person")
                .build();

        when(chartQueryExecutor.execute(intent, null, null))
                .thenReturn(ChartQueryResult.builder()
                        .rows(List.of(
                                Map.of("name", "2024-01", "Teodor", new BigDecimal("100"), "Maria", new BigDecimal("150")),
                                Map.of("name", "2024-02", "Teodor", new BigDecimal("200"), "Maria", new BigDecimal("120"))
                        ))
                        .seriesNames(List.of("Teodor", "Maria"))
                        .labelKey("name")
                        .build());

        ChartResponseDTO result = chartGenerationService.generate(intent);

        assertNotNull(result);
        assertEquals(2, result.getPayload().getData().size());
        assertEquals(List.of("Teodor", "Maria"), result.getPayload().getDataKeys());
    }

    @Test
    void generate_shouldHandleEmptyResult() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .chartType("pie")
                .title("Empty Data")
                .aggregation("sum")
                .groupBy("category")
                .build();

        when(chartQueryExecutor.execute(intent, null, null))
                .thenReturn(ChartQueryResult.builder()
                        .rows(List.of())
                        .seriesNames(List.of())
                        .labelKey("name")
                        .build());

        ChartResponseDTO result = chartGenerationService.generate(intent);

        assertNotNull(result);
        assertTrue(result.getPayload().getData().isEmpty());
        assertTrue(result.getMessage().contains("Nu am găsit"));
    }

    @Test
    void generate_shouldHandleNullFilters() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .chartType("bar")
                .title("All Expenses")
                .aggregation("sum")
                .groupBy("category")
                .filters(null)
                .build();

        when(chartQueryExecutor.execute(intent, null, null))
                .thenReturn(ChartQueryResult.builder()
                        .rows(List.of(Map.of("name", "food", "value", new BigDecimal("50"))))
                        .seriesNames(List.of("value"))
                        .labelKey("name")
                        .build());

        ChartResponseDTO result = chartGenerationService.generate(intent);

        assertNotNull(result);
        verifyNoInteractions(semanticExpansionService);
    }

    @Test
    void generate_shouldExpandLocations_whenLocationFilterPresent() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .chartType("bar")
                .title("Location Expenses")
                .aggregation("sum")
                .groupBy("category")
                .filters(ChartFilters.builder()
                        .location("mall")
                        .build())
                .build();

        when(semanticExpansionService.expandLocations("mall")).thenReturn(List.of("Afi", "Baneasa"));
        when(chartQueryExecutor.execute(intent, null, List.of("Afi", "Baneasa")))
                .thenReturn(ChartQueryResult.builder()
                        .rows(List.of(Map.of("name", "shopping", "value", new BigDecimal("300"))))
                        .seriesNames(List.of("value"))
                        .labelKey("name")
                        .build());

        ChartResponseDTO result = chartGenerationService.generate(intent);

        assertNotNull(result);
        verify(semanticExpansionService).expandLocations("mall");
    }

    @Test
    void generate_shouldIncludeExplanationMessage() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .chartType("bar")
                .title("Top Expenses")
                .aggregation("sum")
                .groupBy("category")
                .build();

        when(chartQueryExecutor.execute(intent, null, null))
                .thenReturn(ChartQueryResult.builder()
                        .rows(List.of(
                                Map.of("name", "food", "value", new BigDecimal("500")),
                                Map.of("name", "transport", "value", new BigDecimal("200")),
                                Map.of("name", "shopping", "value", new BigDecimal("100"))
                        ))
                        .seriesNames(List.of("value"))
                        .labelKey("name")
                        .build());

        ChartResponseDTO result = chartGenerationService.generate(intent);

        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("food"));
        assertTrue(result.getMessage().contains("500"));
    }
}
