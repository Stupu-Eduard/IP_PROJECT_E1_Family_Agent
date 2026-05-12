package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.ChartFilters;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import com.familie.cheltuieli_familie.model.ChartQueryResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChartQueryExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ChartQueryExecutor chartQueryExecutor;

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldBuildSimpleQueryWithoutSeries() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("sum")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(
                        Map.of("label", "food", "total", new BigDecimal("100")),
                        Map.of("label", "transport", "total", new BigDecimal("200"))
                ));

        ChartQueryResult result = chartQueryExecutor.execute(intent, null, null);

        assertNotNull(result);
        assertEquals(2, result.getRows().size());
        assertEquals(List.of("value"), result.getSeriesNames());
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldBuildQueryWithSeries() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("month")
                .seriesBy("person")
                .aggregation("sum")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(
                        Map.of("label", "2024-01", "series", "Teodor", "total", new BigDecimal("100")),
                        Map.of("label", "2024-01", "series", "Maria", "total", new BigDecimal("150"))
                ));

        ChartQueryResult result = chartQueryExecutor.execute(intent, null, null);

        assertNotNull(result);
        assertEquals(1, result.getRows().size());
        assertEquals(List.of("Teodor", "Maria"), result.getSeriesNames());
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldApplyCategoryFilter() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("sum")
                .filters(ChartFilters.builder()
                        .category("food")
                        .build())
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "food", "total", new BigDecimal("100"))));

        ChartQueryResult result = chartQueryExecutor.execute(intent, List.of("food", "mancare"), null);

        assertNotNull(result);
        verify(jdbcTemplate).query(contains("category IN"), any(RowMapper.class), any(Object[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldApplyLocationFilter() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("sum")
                .filters(ChartFilters.builder()
                        .location("Bucuresti")
                        .build())
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "food", "total", new BigDecimal("100"))));

        chartQueryExecutor.execute(intent, null, List.of("Bucuresti", "Cluj"));

        verify(jdbcTemplate).query(contains("location IN"), any(RowMapper.class), any(Object[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldApplyPersonFilter() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("sum")
                .filters(ChartFilters.builder()
                        .person("Teodor")
                        .build())
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "food", "total", new BigDecimal("100"))));

        chartQueryExecutor.execute(intent, null, null);

        verify(jdbcTemplate).query(contains("person = ?"), any(RowMapper.class), any(Object[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldReturnEmptyResultForNoData() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("sum")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        ChartQueryResult result = chartQueryExecutor.execute(intent, null, null);

        assertNotNull(result);
        assertTrue(result.getRows().isEmpty());
        assertTrue(result.getSeriesNames().isEmpty());
    }

    @Test
    void validateIntent_shouldThrowForInvalidGroupBy() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("invalid_column")
                .aggregation("sum")
                .build();

        assertThrows(IllegalArgumentException.class, () -> chartQueryExecutor.execute(intent, null, null));
    }

    @Test
    void validateIntent_shouldThrowForInvalidSeriesBy() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .seriesBy("invalid_column")
                .aggregation("sum")
                .build();

        assertThrows(IllegalArgumentException.class, () -> chartQueryExecutor.execute(intent, null, null));
    }

    @Test
    void validateIntent_shouldThrowForInvalidAggregation() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("invalid")
                .build();

        assertThrows(IllegalArgumentException.class, () -> chartQueryExecutor.execute(intent, null, null));
    }

    @SuppressWarnings("unchecked")
    @Test
    void buildLabelColumn_shouldHandleMonth() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("month")
                .aggregation("sum")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        chartQueryExecutor.execute(intent, null, null);

        verify(jdbcTemplate).query(contains("EXTRACT(YEAR FROM date)"), any(RowMapper.class), any(Object[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void buildLabelColumn_shouldHandleYear() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("year")
                .aggregation("sum")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        chartQueryExecutor.execute(intent, null, null);

        verify(jdbcTemplate).query(contains("EXTRACT(YEAR FROM date)"), any(RowMapper.class), any(Object[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void resolveFilterList_shouldPreferExpandedList() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("sum")
                .filters(ChartFilters.builder()
                        .category("food")
                        .build())
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "food", "total", new BigDecimal("100"))));

        chartQueryExecutor.execute(intent, List.of("food", "mancare"), null);

        verify(jdbcTemplate).query(contains("category IN"), any(RowMapper.class), any(Object[].class));
    }
}
