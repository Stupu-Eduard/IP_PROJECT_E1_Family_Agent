package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.ChartFilters;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import com.familie.cheltuieli_familie.model.ChartQueryResult;
import com.familie.cheltuieli_familie.security.util.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChartQueryExecutorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private ChartQueryExecutor chartQueryExecutor;

    @BeforeEach
    void setUp() {
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
    }

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
    void execute_shouldBuildQueryWithLocationSeries() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .seriesBy("location")
                .aggregation("sum")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(
                        Map.of("label", "food", "series", "Bucuresti", "total", new BigDecimal("100"))
                ));

        ChartQueryResult result = chartQueryExecutor.execute(intent, null, null);

        assertNotNull(result);
        assertEquals(1, result.getRows().size());
        assertEquals(List.of("Bucuresti"), result.getSeriesNames());
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @CsvSource({
            "category, sum",
            "person, sum",
            "location, sum"
    })
    void execute_shouldBuildQueryWithGroupBy(String groupBy, String aggregation) {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy(groupBy)
                .aggregation(aggregation)
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "test", "total", new BigDecimal("100"))));

        ChartQueryResult result = chartQueryExecutor.execute(intent, null, null);

        assertNotNull(result);
        assertEquals(1, result.getRows().size());
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
        verify(jdbcTemplate).query(contains("c.name IN"), any(RowMapper.class), any(Object[].class));
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

        verify(jdbcTemplate).query(contains("l.store IN"), any(RowMapper.class), any(Object[].class));
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

        verify(jdbcTemplate).query(contains("u.name = ?"), any(RowMapper.class), any(Object[].class));
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

        verify(jdbcTemplate).query(contains("EXTRACT(YEAR FROM e.expense_date)"), any(RowMapper.class), any(Object[].class));
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

        verify(jdbcTemplate).query(contains("EXTRACT(YEAR FROM e.expense_date)"), any(RowMapper.class), any(Object[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldApplyDateRangeFilter() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("sum")
                .filters(ChartFilters.builder()
                        .dateRange("this_month")
                        .build())
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "food", "total", new BigDecimal("100"))));

        chartQueryExecutor.execute(intent, null, null);

        verify(jdbcTemplate).query(contains("e.expense_date >="), any(RowMapper.class), any(Object[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldApplyCountAggregation() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("count")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "food", "total", new BigDecimal("5"))));

        chartQueryExecutor.execute(intent, null, null);

        verify(jdbcTemplate).query(contains("COUNT(e.amount)"), any(RowMapper.class), any(Object[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldApplyAvgAggregation() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("avg")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "food", "total", new BigDecimal("50"))));

        chartQueryExecutor.execute(intent, null, null);

        verify(jdbcTemplate).query(contains("AVG(e.amount)"), any(RowMapper.class), any(Object[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldApplySumAggregation() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("sum")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "food", "total", new BigDecimal("100"))));

        chartQueryExecutor.execute(intent, null, null);

        verify(jdbcTemplate).query(contains("SUM(e.amount)"), any(RowMapper.class), any(Object[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldApplyFamilyScope() {
        when(securityService.resolveScope()).thenReturn(new Long[]{1L, null});

        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("sum")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "food", "total", new BigDecimal("100"))));

        ChartQueryResult result = chartQueryExecutor.execute(intent, null, null);

        assertNotNull(result);
        verify(jdbcTemplate).query(contains("e.family_id = ?"), any(RowMapper.class), any(Object[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldApplyNoScope() {
        when(securityService.resolveScope()).thenReturn(new Long[]{null, null});

        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("sum")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "food", "total", new BigDecimal("100"))));

        ChartQueryResult result = chartQueryExecutor.execute(intent, null, null);

        assertNotNull(result);
        verify(jdbcTemplate).query(contains("WHERE 1=1"), any(RowMapper.class), any(Object[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldBuildQueryWithYearSeries() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .seriesBy("year")
                .aggregation("sum")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "food", "series", "2024", "total", new BigDecimal("100"))));

        ChartQueryResult result = chartQueryExecutor.execute(intent, null, null);

        assertNotNull(result);
        assertEquals(1, result.getRows().size());
        assertEquals(List.of("2024"), result.getSeriesNames());
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldBuildQueryWithMonthSeries() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("person")
                .seriesBy("month")
                .aggregation("sum")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "Teodor", "series", "2024-01", "total", new BigDecimal("100"))));

        ChartQueryResult result = chartQueryExecutor.execute(intent, null, null);

        assertNotNull(result);
        assertEquals(1, result.getRows().size());
        assertEquals(List.of("2024-01"), result.getSeriesNames());
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_shouldHandleNullFilters() {
        ChartQueryIntent intent = ChartQueryIntent.builder()
                .groupBy("category")
                .aggregation("sum")
                .filters(null)
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(Map.of("label", "food", "total", new BigDecimal("100"))));

        ChartQueryResult result = chartQueryExecutor.execute(intent, null, null);

        assertNotNull(result);
        assertEquals(1, result.getRows().size());
    }
}
