package com.familie.cheltuieli_familie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familie.cheltuieli_familie.model.ChartFilters;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VisualIntentExtractorTest {

    private VisualIntentExtractor extractor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @BeforeEach
    void setUp() {
        extractor = new VisualIntentExtractor(chatLanguageModel);
    }

    @Test
    void parseIntent_shouldReturnDefaultsForEmptyJson() {
        ChartQueryIntent intent = extractor.parseIntent("{}");

        assertEquals("text", intent.getResponseType());
        assertEquals("bar", intent.getChartType());
        assertEquals("sum", intent.getAggregation());
        assertEquals("category", intent.getGroupBy());
        assertNull(intent.getSeriesBy());
        assertEquals("Analiză cheltuieli", intent.getTitle());
    }

    @Test
    void parseIntent_shouldParseValidJson() throws Exception {
        String json = objectMapper.writeValueAsString(ChartQueryIntent.builder()
                .responseType("chart")
                .chartType("pie")
                .aggregation("avg")
                .groupBy("person")
                .seriesBy("category")
                .title("Test Title")
                .filters(ChartFilters.builder()
                        .category("food")
                        .person("Teodor")
                        .build())
                .build());

        ChartQueryIntent intent = extractor.parseIntent(json);

        assertEquals("chart", intent.getResponseType());
        assertEquals("pie", intent.getChartType());
        assertEquals("avg", intent.getAggregation());
        assertEquals("person", intent.getGroupBy());
        assertEquals("category", intent.getSeriesBy());
        assertEquals("Test Title", intent.getTitle());
        assertNotNull(intent.getFilters());
        assertEquals("food", intent.getFilters().getCategory());
        assertEquals("Teodor", intent.getFilters().getPerson());
    }

    @Test
    void parseIntent_shouldSanitizeInvalidResponseType() {
        ChartQueryIntent intent = extractor.parseIntent("{\"responseType\": \"invalid\"}");
        assertEquals("text", intent.getResponseType());
    }

    @Test
    void parseIntent_shouldSanitizeInvalidChartType() {
        ChartQueryIntent intent = extractor.parseIntent("{\"chartType\": \"invalid\"}");
        assertEquals("bar", intent.getChartType());
    }

    @Test
    void parseIntent_shouldSanitizeInvalidAggregation() {
        ChartQueryIntent intent = extractor.parseIntent("{\"aggregation\": \"invalid\"}");
        assertEquals("sum", intent.getAggregation());
    }

    @Test
    void parseIntent_shouldSanitizeInvalidGroupBy() {
        ChartQueryIntent intent = extractor.parseIntent("{\"groupBy\": \"invalid\"}");
        assertEquals("category", intent.getGroupBy());
    }

    @Test
    void parseIntent_shouldSanitizeInvalidSeriesBy() {
        ChartQueryIntent intent = extractor.parseIntent("{\"seriesBy\": \"invalid\"}");
        assertNull(intent.getSeriesBy());
    }

    @Test
    void parseIntent_shouldHandleNullSeriesBy() {
        ChartQueryIntent intent = extractor.parseIntent("{\"seriesBy\": null}");
        assertNull(intent.getSeriesBy());
    }

    @Test
    void parseIntent_shouldParseFilters() {
        String json = "{\"filters\": {\"category\": \"food\", \"person\": \"Teodor\", \"dateRange\": \"this_month\", \"location\": \"Bucuresti\"}}";
        ChartQueryIntent intent = extractor.parseIntent(json);

        assertNotNull(intent.getFilters());
        assertEquals("food", intent.getFilters().getCategory());
        assertEquals("Teodor", intent.getFilters().getPerson());
        assertEquals("this_month", intent.getFilters().getDateRange());
        assertEquals("Bucuresti", intent.getFilters().getLocation());
    }

    @Test
    void parseIntent_shouldHandleBlankFilters() {
        String json = "{\"filters\": {\"category\": \"\", \"person\": \"   \"}}";
        ChartQueryIntent intent = extractor.parseIntent(json);

        assertNotNull(intent.getFilters());
        assertNull(intent.getFilters().getCategory());
        assertNull(intent.getFilters().getPerson());
    }

    @Test
    void parseIntent_shouldHandleMissingFilters() {
        ChartQueryIntent intent = extractor.parseIntent("{}");
        assertNotNull(intent.getFilters());
        assertNull(intent.getFilters().getCategory());
    }

    @Test
    void parseIntent_shouldReturnTextFallbackOnInvalidJson() {
        ChartQueryIntent intent = extractor.parseIntent("not valid json");
        assertEquals("text", intent.getResponseType());
        assertNull(intent.getChartType());
    }

    @Test
    void stripMarkdownFences_shouldRemoveJsonFence() {
        String raw = "```json\n{\"responseType\": \"chart\"}\n```";
        String cleaned = extractor.stripMarkdownFences(raw);
        assertEquals("{\"responseType\": \"chart\"}", cleaned.trim());
    }

    @Test
    void stripMarkdownFences_shouldRemoveGenericFence() {
        String raw = "```\n{\"responseType\": \"chart\"}\n```";
        String cleaned = extractor.stripMarkdownFences(raw);
        assertEquals("{\"responseType\": \"chart\"}", cleaned);
    }

    @Test
    void stripMarkdownFences_shouldReturnPlainText() {
        String raw = "{\"responseType\": \"chart\"}";
        String cleaned = extractor.stripMarkdownFences(raw);
        assertEquals(raw, cleaned);
    }

    @Test
    void stripMarkdownFences_shouldHandleNull() {
        assertNull(extractor.stripMarkdownFences(null));
    }
}
