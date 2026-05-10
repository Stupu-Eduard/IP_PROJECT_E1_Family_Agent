package com.proiect.service;
import org.springframework.test.context.ContextConfiguration;

import com.proiect.model.ChartQueryIntent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VisualIntentExtractorTest {

    @Test
    void testParseChartIntent() {
        String json = """
            {
              "responseType": "chart",
              "chartType": "bar",
              "aggregation": "sum",
              "groupBy": "person",
              "seriesBy": null,
              "title": "Cheltuieli pe persoane",
              "filters": {
                "category": "mancare",
                "person": null,
                "dateRange": "last_3_months",
                "location": null
              }
            }
            """;

        VisualIntentExtractor extractor = new VisualIntentExtractor(null);
        ChartQueryIntent intent = extractor.parseIntent(json);

        assertEquals("chart", intent.getResponseType());
        assertEquals("bar", intent.getChartType());
        assertEquals("sum", intent.getAggregation());
        assertEquals("person", intent.getGroupBy());
        assertNull(intent.getSeriesBy());
        assertEquals("Cheltuieli pe persoane", intent.getTitle());
        assertNotNull(intent.getFilters());
        assertEquals("mancare", intent.getFilters().getCategory());
        assertEquals("last_3_months", intent.getFilters().getDateRange());
    }

    @Test
    void testParseTextIntent() {
        String json = """
            {
              "responseType": "text",
              "chartType": null,
              "aggregation": "sum",
              "groupBy": "category",
              "seriesBy": null,
              "title": "Răspuns text",
              "filters": {}
            }
            """;

        VisualIntentExtractor extractor = new VisualIntentExtractor(null);
        ChartQueryIntent intent = extractor.parseIntent(json);

        assertEquals("text", intent.getResponseType());
    }

    @Test
    void testParseWithMarkdownFences() {
        String raw = """
            ```json
            {
              "responseType": "chart",
              "chartType": "pie",
              "aggregation": "sum",
              "groupBy": "category",
              "seriesBy": null,
              "title": "Pe categorii",
              "filters": {}
            }
            ```
            """;

        VisualIntentExtractor extractor = new VisualIntentExtractor(null);
        String json = extractor.stripMarkdownFences(raw);
        ChartQueryIntent intent = extractor.parseIntent(json);

        assertEquals("chart", intent.getResponseType());
        assertEquals("pie", intent.getChartType());
    }

    @Test
    void testParseInvalidValuesFallback() {
        String json = """
            {
              "responseType": "invalid",
              "chartType": "invalid",
              "aggregation": "invalid",
              "groupBy": "invalid",
              "seriesBy": "invalid",
              "title": "Test",
              "filters": {}
            }
            """;

        VisualIntentExtractor extractor = new VisualIntentExtractor(null);
        ChartQueryIntent intent = extractor.parseIntent(json);

        assertEquals("text", intent.getResponseType());
        assertEquals("bar", intent.getChartType());
        assertEquals("sum", intent.getAggregation());
        assertEquals("category", intent.getGroupBy());
        assertNull(intent.getSeriesBy());
    }

    @Test
    void testParseMalformedJsonFallback() {
        VisualIntentExtractor extractor = new VisualIntentExtractor(null);
        ChartQueryIntent intent = extractor.parseIntent("not json");

        assertEquals("text", intent.getResponseType());
    }

    @Test
    void testParseEmptyJsonFallback() {
        VisualIntentExtractor extractor = new VisualIntentExtractor(null);
        ChartQueryIntent intent = extractor.parseIntent("{}");

        assertEquals("text", intent.getResponseType());
        assertEquals("bar", intent.getChartType());
        assertEquals("sum", intent.getAggregation());
    }
}
