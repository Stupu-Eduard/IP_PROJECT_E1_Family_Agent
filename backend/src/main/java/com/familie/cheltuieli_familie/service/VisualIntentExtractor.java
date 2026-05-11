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

    public VisualIntentExtractor(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {2000, 4000};

    private static final String DEFAULT_GROUP_BY = "category";
    private static final String DEFAULT_SERIES_BY = "person";

    private static final Set<String> ALLOWED_RESPONSE_TYPES = Set.of("text", "chart");
    private static final Set<String> ALLOWED_CHART_TYPES = Set.of("bar", "pie", "line");
    private static final Set<String> ALLOWED_AGGREGATIONS = Set.of("sum", "count", "avg");
    private static final Set<String> ALLOWED_GROUP_BY = Set.of(DEFAULT_GROUP_BY, "person", "month", "year", "location");
    private static final Set<String> ALLOWED_SERIES_BY = Set.of(DEFAULT_SERIES_BY, "category");

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
            - responseType: "text" sau "chart"
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

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
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
                if (attempt < MAX_RETRIES) {
                    long delay = RETRY_DELAYS_MS[attempt - 1];
                    log.info("Retrying intent extraction in {} ms...", delay);
                    try {
                        Thread.sleep(delay);
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

            String responseType = root.path("responseType").asText("text");
            if (!ALLOWED_RESPONSE_TYPES.contains(responseType)) {
                responseType = "text";
            }

            String chartType = root.path("chartType").asText("bar");
            if (!ALLOWED_CHART_TYPES.contains(chartType)) {
                chartType = "bar";
            }

            String aggregation = root.path("aggregation").asText("sum");
            if (!ALLOWED_AGGREGATIONS.contains(aggregation)) {
                aggregation = "sum";
            }

            String groupBy = root.path("groupBy").asText(DEFAULT_GROUP_BY);
            if (!ALLOWED_GROUP_BY.contains(groupBy)) {
                groupBy = DEFAULT_GROUP_BY;
            }

            JsonNode seriesByNode = root.path("seriesBy");
            String seriesBy = seriesByNode.isNull() ? null : seriesByNode.asText();
            if (seriesBy != null && !ALLOWED_SERIES_BY.contains(seriesBy)) {
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
                    .responseType("text")
                    .build();
        }
    }

    private ChartFilters parseFilters(JsonNode filtersNode) {
        if (filtersNode == null || filtersNode.isMissingNode()) {
            return ChartFilters.builder().build();
        }

        return ChartFilters.builder()
                .category(nullIfBlank(filtersNode.path("category").asText(null)))
                .person(nullIfBlank(filtersNode.path("person").asText(null)))
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
