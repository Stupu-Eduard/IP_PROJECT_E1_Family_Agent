package com.proiect.service;

import com.proiect.dto.response.ChartPayload;
import com.proiect.dto.response.ChartResponseDTO;
import com.proiect.model.ChartFilters;
import com.proiect.model.ChartQueryIntent;
import com.proiect.model.ChartQueryResult;
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

    @Test
    void testGenerateChart() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .responseType("chart")
                .chartType("bar")
                .aggregation("sum")
                .groupBy("person")
                .seriesBy(null)
                .title("Comparație")
                .filters(ChartFilters.builder().category("mancare").build())
                .build();

        List<String> expandedCategories = List.of("mancare", "grocery");
        when(semanticExpansionService.expandCategories("mancare")).thenReturn(expandedCategories);

        ChartQueryResult queryResult = ChartQueryResult.builder()
                .rows(List.of(
                        Map.of("name", "Teodor", "value", new BigDecimal("1200")),
                        Map.of("name", "Maria", "value", new BigDecimal("900"))
                ))
                .seriesNames(List.of("value"))
                .labelKey("name")
                .build();

        when(chartQueryExecutor.execute(intent, expandedCategories, null)).thenReturn(queryResult);

        ChartResponseDTO response = chartGenerationService.generate("Compară cheltuielile", intent);

        assertEquals("chart", response.getType());
        assertNotNull(response.getPayload());
        assertEquals("bar", response.getPayload().getChartType());
        assertEquals("Comparație", response.getPayload().getTitle());
        assertEquals(2, response.getPayload().getData().size());
        assertTrue(response.getMessage().contains("Teodor"));
    }

    @Test
    void testGenerateChartWithLocationFilter() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .responseType("chart")
                .chartType("pie")
                .aggregation("sum")
                .groupBy("category")
                .seriesBy(null)
                .title("Pe categorii")
                .filters(ChartFilters.builder().location("Lidl").build())
                .build();

        List<String> expandedLocations = List.of("Lidl", "Kaufland");
        when(semanticExpansionService.expandLocations("Lidl")).thenReturn(expandedLocations);

        ChartQueryResult queryResult = ChartQueryResult.builder()
                .rows(List.of(Map.of("name", "Mancare", "value", new BigDecimal("500"))))
                .seriesNames(List.of("value"))
                .labelKey("name")
                .build();

        when(chartQueryExecutor.execute(intent, null, expandedLocations)).thenReturn(queryResult);

        ChartResponseDTO response = chartGenerationService.generate("Ce am cumpărat", intent);

        assertEquals("pie", response.getPayload().getChartType());
    }

    @Test
    void testGenerateEmptyData() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .responseType("chart")
                .chartType("bar")
                .aggregation("sum")
                .groupBy("person")
                .title("Test")
                .build();

        ChartQueryResult queryResult = ChartQueryResult.builder()
                .rows(List.of())
                .seriesNames(List.of())
                .labelKey("name")
                .build();

        when(chartQueryExecutor.execute(intent, null, null)).thenReturn(queryResult);

        ChartResponseDTO response = chartGenerationService.generate("Test", intent);

        assertNotNull(response.getPayload());
        assertTrue(response.getPayload().getData().isEmpty());
        assertTrue(response.getMessage().contains("Nu am găsit"));
    }
}
