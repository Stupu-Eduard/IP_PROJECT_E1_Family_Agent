package com.proiect.service;

import com.proiect.model.ChartQueryIntent;
import com.proiect.model.ChartQueryResult;
import com.proiect.util.DateRangeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChartQueryExecutor {

    private final JdbcTemplate jdbcTemplate;

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "person", "category", "location", "month", "year"
    );
    private static final Set<String> ALLOWED_AGGREGATIONS = Set.of("SUM", "COUNT", "AVG");

    public ChartQueryResult execute(ChartQueryIntent intent, List<String> expandedCategories, List<String> expandedLocations) {
        validateIntent(intent);

        String groupByColumn = intent.getGroupBy();
        String seriesColumn = intent.getSeriesBy();
        String aggregation = intent.getAggregation().toUpperCase();

        String labelExpression = buildLabelColumn(groupByColumn);

        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(labelExpression).append(" AS label");

        if (seriesColumn != null) {
            sql.append(", ").append(seriesColumn).append(" AS series");
        }

        sql.append(", ").append(aggregation).append("(amount) AS total ");
        sql.append("FROM expenses WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        applyFilters(sql, params, intent, expandedCategories, expandedLocations);

        sql.append("GROUP BY ").append(labelExpression);
        if (seriesColumn != null) {
            sql.append(", ").append(seriesColumn);
        }
        sql.append(" ORDER BY ").append(labelExpression);

        log.info("Executing chart SQL: {} with params: {}", sql, params);

        List<Map<String, Object>> rawRows = jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                this::mapRow
        );

        return pivotAndFormat(rawRows, seriesColumn);
    }

    private void validateIntent(ChartQueryIntent intent) {
        if (intent.getGroupBy() != null && !ALLOWED_COLUMNS.contains(intent.getGroupBy())) {
            throw new IllegalArgumentException("Invalid groupBy column: " + intent.getGroupBy());
        }
        if (intent.getSeriesBy() != null && !ALLOWED_COLUMNS.contains(intent.getSeriesBy())) {
            throw new IllegalArgumentException("Invalid seriesBy column: " + intent.getSeriesBy());
        }
        String aggregation = intent.getAggregation().toUpperCase();
        if (!ALLOWED_AGGREGATIONS.contains(aggregation)) {
            throw new IllegalArgumentException("Invalid aggregation: " + intent.getAggregation());
        }
    }

    private String buildLabelColumn(String groupBy) {
        return switch (groupBy) {
            case "month" ->
                    "CONCAT(CAST(EXTRACT(YEAR FROM date) AS VARCHAR), '-', LPAD(CAST(EXTRACT(MONTH FROM date) AS VARCHAR), 2, '0'))";
            case "year" -> "CAST(EXTRACT(YEAR FROM date) AS VARCHAR)";
            default -> groupBy;
        };
    }

    private void applyFilters(StringBuilder sql, List<Object> params, ChartQueryIntent intent,
                              List<String> expandedCategories, List<String> expandedLocations) {
        var filters = intent.getFilters();
        if (filters == null) return;

        // Category filter (use expanded list if available)
        List<String> categories = (expandedCategories != null && !expandedCategories.isEmpty())
                ? expandedCategories
                : (filters.getCategory() != null ? List.of(filters.getCategory()) : null);

        if (categories != null && !categories.isEmpty()) {
            sql.append(" AND category IN (");
            sql.append(String.join(",", Collections.nCopies(categories.size(), "?")));
            sql.append(")");
            params.addAll(categories);
        }

        // Location filter
        List<String> locations = (expandedLocations != null && !expandedLocations.isEmpty())
                ? expandedLocations
                : (filters.getLocation() != null ? List.of(filters.getLocation()) : null);

        if (locations != null && !locations.isEmpty()) {
            sql.append(" AND location IN (");
            sql.append(String.join(",", Collections.nCopies(locations.size(), "?")));
            sql.append(")");
            params.addAll(locations);
        }

        // Person filter
        if (filters.getPerson() != null && !filters.getPerson().isBlank()) {
            sql.append(" AND person = ?");
            params.add(filters.getPerson());
        }

        // Date range filter
        LocalDate[] range = DateRangeUtil.parseDateRange(filters.getDateRange());
        if (range[0] != null) {
            sql.append(" AND date >= ?");
            params.add(range[0]);
        }
        if (range[1] != null) {
            sql.append(" AND date <= ?");
            params.add(range[1]);
        }
    }

    private Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", rs.getString("label"));
        if (hasColumn(rs, "series")) {
            row.put("series", rs.getString("series"));
        }
        row.put("value", rs.getBigDecimal("total"));
        return row;
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private ChartQueryResult pivotAndFormat(List<Map<String, Object>> rawRows, String seriesColumn) {
        if (rawRows.isEmpty()) {
            return ChartQueryResult.builder()
                    .rows(new ArrayList<>())
                    .seriesNames(new ArrayList<>())
                    .labelKey("name")
                    .build();
        }

        // If no series, simple single-series result
        if (seriesColumn == null) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map<String, Object> raw : rawRows) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", raw.get("label"));
                row.put("value", raw.get("value"));
                rows.add(row);
            }
            return ChartQueryResult.builder()
                    .rows(rows)
                    .seriesNames(List.of("value"))
                    .labelKey("name")
                    .build();
        }

        // Multi-series: pivot raw rows into Recharts format
        // Raw: {label: "2024-01", series: "Teodor", value: 1200}
        // Target: {name: "2024-01", Teodor: 1200, Maria: 900}
        Set<String> seriesNames = new LinkedHashSet<>();
        Map<String, Map<String, Object>> pivot = new LinkedHashMap<>();

        for (Map<String, Object> raw : rawRows) {
            String label = String.valueOf(raw.get("label"));
            String series = String.valueOf(raw.get("series"));
            BigDecimal value = (BigDecimal) raw.get("value");

            seriesNames.add(series);
            pivot.computeIfAbsent(label, k -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", k);
                return row;
            }).put(series, value);
        }

        return ChartQueryResult.builder()
                .rows(new ArrayList<>(pivot.values()))
                .seriesNames(new ArrayList<>(seriesNames))
                .labelKey("name")
                .build();
    }
}
