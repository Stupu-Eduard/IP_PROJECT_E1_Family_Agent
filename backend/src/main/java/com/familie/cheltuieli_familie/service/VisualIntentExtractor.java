package com.familie.cheltuieli_familie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familie.cheltuieli_familie.model.ChartFilters;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class VisualIntentExtractor {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final int maxRetries;
    private final long[] retryDelaysMs;
    private final String defaultGroupBy;
    private final String defaultSeriesBy;

    private static final String DEFAULT_RESPONSE_TYPE = "data_query";
    private static final Set<String> ALLOWED_RESPONSE_TYPES = Set.of("conversation", DEFAULT_RESPONSE_TYPE, "chart");
    private static final Set<String> ALLOWED_CHART_TYPES = Set.of("bar", "pie", "line");
    private static final Set<String> ALLOWED_AGGREGATIONS = Set.of("sum", "count", "avg");

    public VisualIntentExtractor(ChatLanguageModel chatLanguageModel,
                                  int maxRetries,
                                  long[] retryDelaysMs,
                                  String defaultGroupBy,
                                  String defaultSeriesBy) {
        this.chatLanguageModel = chatLanguageModel;
        this.maxRetries = maxRetries;
        this.retryDelaysMs = retryDelaysMs;
        this.defaultGroupBy = defaultGroupBy;
        this.defaultSeriesBy = defaultSeriesBy;
    }

    private Set<String> allowedGroupBy() {
        return Set.of(defaultGroupBy, defaultSeriesBy, "month", "year", "location");
    }

    private Set<String> allowedSeriesBy() {
        return Set.of(defaultSeriesBy, defaultGroupBy);
    }

    interface IntentAssistant {
        @SystemMessage("""
            Ești un clasificator de intenții pentru un asistent financiar de familie.
            BAZA DE DATE are următoarea structură:
            
            TABEL: expenses
            - id (BIGINT, PK)
            - amount (DECIMAL) — suma cheltuită
            - category (VARCHAR) — categoria cheltuielii (ex: "mancare", "transport", "shopping")
            - location (VARCHAR) — locația (ex: "Lidl", "Bucuresti")
            - person (VARCHAR) — persoana care a cheltuit (ex: "Teodor", "Maria")
            - date (DATE) — data tranzacției
            - raw_input (VARCHAR) — textul brut extras
            
            REGULI:
            1. Analizează întrebarea și returnează DOAR un obiect JSON.
            2. Nu adăuga câmpuri care nu există în tabel.
            3. Nu presupune existența unor coloane precum "budget", "income", "savings".
            
            Câmpuri JSON obligatorii:
            - responseType: "conversation" | "data_query" | "chart"
              - "conversation" = salutări, small talk, întrebări generale fără legătură cu datele
              - "data_query" = întrebări despre cheltuieli, analize, sume, comparări (necesită RAG)
              - "chart" = cereri de grafic sau vizualizare
            - chartType: "bar" | "pie" | "line" (doar dacă responseType="chart")
            - aggregation: "sum" | "count" | "avg"
            - groupBy: "person" | "category" | "month" | "year" | "location"
            - seriesBy: "person" | "category" | null
            - title: titlu în română
            - filters: { category, person, dateRange, location }
            
            Pentru dateRange, folosește: "last_3_months", "this_month", "this_year",
            sau un obiect { from: "YYYY-MM-DD", to: "YYYY-MM-DD" }.
            """)
        String extractIntent(@UserMessage String userMessage);
    }

    public ChartQueryIntent extract(String userMessage) {
        String jsonResult = callWithRetry(userMessage);
        return parseIntent(jsonResult);
    }

    private String callWithRetry(String userMessage) {
        IntentAssistant assistant = AiServices.create(IntentAssistant.class, chatLanguageModel);
        String lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String rawResult = assistant.extractIntent(userMessage);
                log.info("Intent extraction raw (attempt {}): {}", attempt, rawResult);
                String jsonResult = stripMarkdownFences(rawResult);
                log.info("Intent extraction cleaned (attempt {}): {}", attempt, jsonResult);

                objectMapper.readTree(jsonResult);
                return jsonResult;
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                lastError = e.getMessage();
                log.warn("Intent extraction attempt {} failed: {}", attempt, lastError);
                if (attempt < maxRetries) {
                    long delay = retryDelaysMs[attempt - 1];
                    log.info("Retrying intent extraction in {} ms...", delay);
                    try {
                        java.util.concurrent.TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Retry interrupted", ie);
                    }
                }
            }
        }

        throw new IllegalStateException("Eroare la extragerea intenției: " + lastError);
    }

    ChartQueryIntent parseIntent(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String responseType = root.path("responseType").asText(DEFAULT_RESPONSE_TYPE);
            if (!ALLOWED_RESPONSE_TYPES.contains(responseType)) {
                responseType = DEFAULT_RESPONSE_TYPE;
            }

            String chartType = root.path("chartType").asText("bar");
            if (!ALLOWED_CHART_TYPES.contains(chartType)) {
                chartType = "bar";
            }

            String aggregation = root.path("aggregation").asText("sum");
            if (!ALLOWED_AGGREGATIONS.contains(aggregation)) {
                aggregation = "sum";
            }

            String groupBy = root.path("groupBy").asText(defaultGroupBy);
            if (!allowedGroupBy().contains(groupBy)) {
                groupBy = defaultGroupBy;
            }

            JsonNode seriesByNode = root.path("seriesBy");
            String seriesBy = seriesByNode.isNull() ? null : seriesByNode.asText();
            if (seriesBy != null && !allowedSeriesBy().contains(seriesBy)) {
                seriesBy = null;
            }

            String title = root.path("title").asText("Analiză cheltuieli");

            ChartFilters filters = parseFilters(root.path("filters"));

            return ChartQueryIntent.builder()
                    .responseType(responseType)
                    .chartType(chartType)
                    .aggregation(aggregation)
                    .groupBy(groupBy)
                    .seriesBy(seriesBy)
                    .title(title)
                    .filters(filters)
                    .build();

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to parse intent JSON: {}", e.getMessage());
            return ChartQueryIntent.builder()
                    .responseType(DEFAULT_RESPONSE_TYPE)
                    .build();
        }
    }

    private ChartFilters parseFilters(JsonNode filtersNode) {
        if (filtersNode == null || filtersNode.isMissingNode()) {
            return ChartFilters.builder().build();
        }

        return ChartFilters.builder()
                .category(nullIfBlank(filtersNode.path(defaultGroupBy).asText(null)))
                .person(nullIfBlank(filtersNode.path(defaultSeriesBy).asText(null)))
                .dateRange(nullIfBlank(filtersNode.path("dateRange").asText(null)))
                .location(nullIfBlank(filtersNode.path("location").asText(null)))
                .build();
    }

    private String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    String stripMarkdownFences(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("```json\\s*", "").replaceFirst("```\\s*", "");
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence >= 0) trimmed = trimmed.substring(0, lastFence).trim();
        }
        return trimmed;
    }
}
