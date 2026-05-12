package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.response.ChartPayload;
import com.familie.cheltuieli_familie.dto.response.ChartResponseDTO;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import com.familie.cheltuieli_familie.model.ChartQueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChartGenerationService {

    private final SemanticExpansionService semanticExpansionService;
    private final ChartQueryExecutor chartQueryExecutor;

    public ChartResponseDTO generate(ChartQueryIntent intent) {
        // 1. Semantic expansion of filters
        List<String> expandedCategories = null;
        List<String> expandedLocations = null;

        if (intent.getFilters() != null) {
            if (intent.getFilters().getCategory() != null) {
                expandedCategories = semanticExpansionService.expandCategories(intent.getFilters().getCategory());
            }
            if (intent.getFilters().getLocation() != null) {
                expandedLocations = semanticExpansionService.expandLocations(intent.getFilters().getLocation());
            }
        }

        // 2. Execute SQL query
        ChartQueryResult result = chartQueryExecutor.execute(intent, expandedCategories, expandedLocations);

        // 3. Build payload
        ChartPayload payload = ChartPayload.builder()
                .chartType(intent.getChartType())
                .title(intent.getTitle())
                .data(result.getRows())
                .dataKeys(result.getSeriesNames())
                .xAxisKey(result.getLabelKey())
                .build();

        // 4. Generate explanation message
        String message = buildExplanationMessage(result, intent);

        return new ChartResponseDTO(message, payload);
    }

    private String buildExplanationMessage(ChartQueryResult result, ChartQueryIntent intent) {
        if (result.getRows() == null || result.getRows().isEmpty()) {
            return "Nu am găsit cheltuieli pentru criteriile selectate.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Iată rezultatele pentru ").append(intent.getTitle().toLowerCase()).append(":");

        // For single-series, list top values
        if (result.getSeriesNames().size() == 1) {
            String dataKey = result.getSeriesNames().get(0);
            List<Map<String, Object>> sorted = result.getRows().stream()
                    .sorted((a, b) -> {
                        BigDecimal va = getValue(b, dataKey);
                        BigDecimal vb = getValue(a, dataKey);
                        return va.compareTo(vb);
                    })
                    .limit(3)
                    .collect(Collectors.toList());

            for (Map<String, Object> row : sorted) {
                String name = String.valueOf(row.get("name"));
                BigDecimal value = getValue(row, dataKey);
                sb.append(String.format(" %s: %s RON;", name, value));
            }
        } else {
            // For multi-series, summarize each series total
            for (String series : result.getSeriesNames()) {
                BigDecimal total = result.getRows().stream()
                        .map(row -> getValue(row, series))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                sb.append(String.format(" %s: %s RON total;", series, total));
            }
        }

        return sb.toString().replaceAll(";$", ".");
    }

    private BigDecimal getValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.ZERO;
    }
}
