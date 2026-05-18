package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.service.QdrantVectorService;
import com.familie.cheltuieli_familie.service.SearchQueryCorrector;
import com.familie.cheltuieli_familie.service.SemanticExpansionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ExpenseSearchController.class)
@ActiveProfiles("test")
class ExpenseSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QdrantVectorService qdrantVectorService;

    @MockBean
    private SearchQueryCorrector searchQueryCorrector;

    @MockBean
    private SemanticExpansionService semanticExpansionService;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private com.familie.cheltuieli_familie.security.util.SecurityService securityService;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

    @Test
    void testSemanticSearch() throws Exception {
        when(searchQueryCorrector.correctQuery("mancare")).thenReturn("mancare");
        when(semanticExpansionService.expandCategories(anyString())).thenReturn(List.of());

        EmbeddedExpense result = EmbeddedExpense.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .category("Mâncare")
                .rawInput("Am platit 100 lei")
                .score(0.95)
                .build();

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(anyString(), anyInt(), any(), any())).thenReturn(new ArrayList<>(List.of(result)));

        mockMvc.perform(post("/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"mancare\", \"topK\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testSemanticSearchWithTypoCorrection() throws Exception {
        when(searchQueryCorrector.correctQuery("mancarre")).thenReturn("mancare");
        when(semanticExpansionService.expandCategories(anyString())).thenReturn(List.of());

        EmbeddedExpense result = EmbeddedExpense.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .category("Mâncare")
                .rawInput("Am platit 100 lei")
                .score(0.92)
                .build();

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar(anyString(), anyInt(), any(), any())).thenReturn(new ArrayList<>(List.of(result)));

        mockMvc.perform(post("/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"mancarre\", \"topK\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Mâncare"));
    }

    @Test
    void testSemanticSearch_noResults_expansionAndSqlFallback() throws Exception {
        when(searchQueryCorrector.correctQuery("query")).thenReturn("query");
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar("query", 15, null, 1L)).thenReturn(new ArrayList<>());
        when(semanticExpansionService.expandCategories("query")).thenReturn(List.of("cat1", "cat2"));
        when(qdrantVectorService.searchSimilar("cat1", 15, null, 1L)).thenReturn(new ArrayList<>());
        when(qdrantVectorService.searchSimilar("cat2", 15, null, 1L)).thenReturn(new ArrayList<>());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(new ArrayList<>());

        mockMvc.perform(post("/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"query\", \"topK\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testSemanticSearch_expansionAddsResults() throws Exception {
        when(searchQueryCorrector.correctQuery("query")).thenReturn("query");
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        EmbeddedExpense base = EmbeddedExpense.builder().id(1L).score(0.5).build();
        when(qdrantVectorService.searchSimilar("query", 15, null, 1L)).thenReturn(new ArrayList<>(List.of(base)));

        EmbeddedExpense expanded = EmbeddedExpense.builder().id(2L).score(0.8).build();
        when(semanticExpansionService.expandCategories("query")).thenReturn(List.of("cat1"));
        when(qdrantVectorService.searchSimilar("cat1", 15, null, 1L)).thenReturn(new ArrayList<>(List.of(expanded)));

        mockMvc.perform(post("/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"query\", \"topK\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void testSemanticSearch_sqlFallbackAddsResults() throws Exception {
        when(searchQueryCorrector.correctQuery("query")).thenReturn("query");
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar("query", 15, null, 1L)).thenReturn(new ArrayList<>());
        when(semanticExpansionService.expandCategories("query")).thenReturn(List.of());

        EmbeddedExpense sqlResult = EmbeddedExpense.builder().id(3L).score(0.3).rawInput("sql match").build();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(new ArrayList<>(List.of(sqlResult)));

        mockMvc.perform(post("/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"query\", \"topK\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].rawInput").value("sql match"));
    }

    @Test
    void testSemanticSearch_keywordSearchException_returnsEmpty() throws Exception {
        when(searchQueryCorrector.correctQuery("query")).thenReturn("query");
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchSimilar("query", 15, null, 1L)).thenReturn(new ArrayList<>());
        when(semanticExpansionService.expandCategories("query")).thenReturn(List.of());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"query\", \"topK\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testSemanticSearch_resultsExceedTopK_truncated() throws Exception {
        when(searchQueryCorrector.correctQuery("test")).thenReturn("test");
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        List<EmbeddedExpense> results = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            results.add(EmbeddedExpense.builder().id((long) i).score(0.9 - i * 0.01).build());
        }
        when(qdrantVectorService.searchSimilar("test", 5, null, 1L)).thenReturn(results);
        when(semanticExpansionService.expandCategories(anyString())).thenReturn(List.of());

        mockMvc.perform(post("/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"test\", \"topK\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void testSemanticSearch_withFamilyScope() throws Exception {
        when(searchQueryCorrector.correctQuery("family")).thenReturn("family");
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, null});
        when(semanticExpansionService.expandCategories(anyString())).thenReturn(List.of());

        EmbeddedExpense result = EmbeddedExpense.builder()
                .id(5L)
                .amount(new BigDecimal("50.00"))
                .score(0.85)
                .build();
        when(qdrantVectorService.searchSimilar("family", 15, 10L, null)).thenReturn(new ArrayList<>(List.of(result)));

        mockMvc.perform(post("/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"family\", \"topK\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void testSemanticSearch_duplicateResults_deduplicated() throws Exception {
        when(searchQueryCorrector.correctQuery("dup")).thenReturn("dup");
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        EmbeddedExpense base = EmbeddedExpense.builder().id(1L).score(0.5).build();
        when(qdrantVectorService.searchSimilar("dup", 15, null, 1L)).thenReturn(new ArrayList<>(List.of(base)));

        when(semanticExpansionService.expandCategories("dup")).thenReturn(List.of("cat1"));
        // Return same ID again - should be deduplicated
        when(qdrantVectorService.searchSimilar("cat1", 15, null, 1L)).thenReturn(new ArrayList<>(List.of(base)));

        mockMvc.perform(post("/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"dup\", \"topK\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testFilteredSearch() throws Exception {
        when(searchQueryCorrector.correctQuery("benzina")).thenReturn("benzina");

        EmbeddedExpense result = EmbeddedExpense.builder()
                .id(2L)
                .amount(new BigDecimal("200.00"))
                .category("Transport")
                .person("Teodor")
                .date(LocalDate.of(2024, 3, 15))
                .rawInput("Am platit 200 lei la benzinărie")
                .score(0.88)
                .build();

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchWithFilter(anyString(), anyInt(), any(QdrantVectorService.SearchFilter.class)))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/v1/search/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"benzina\", \"topK\": 15, \"category\": \"Transport\", \"person\": \"Teodor\", \"from\": \"2024-03-01\", \"to\": \"2024-03-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Transport"));
    }

    @Test
    void testFilteredSearch_withNullFilters() throws Exception {
        when(searchQueryCorrector.correctQuery("test")).thenReturn("test");
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        when(qdrantVectorService.searchWithFilter("test", 15, new QdrantVectorService.SearchFilter(null, null, null, null, null, 1L)))
                .thenReturn(List.of());

        mockMvc.perform(post("/v1/search/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"test\", \"topK\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testFilteredSearch_withFamilyScope() throws Exception {
        when(searchQueryCorrector.correctQuery("test")).thenReturn("test");
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, null});
        EmbeddedExpense result = EmbeddedExpense.builder().id(5L).category("Food").build();
        when(qdrantVectorService.searchWithFilter("test", 15, new QdrantVectorService.SearchFilter("Food", "Alice", null, null, 10L, null)))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/v1/search/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"test\", \"topK\": 15, \"category\": \"Food\", \"person\": \"Alice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void testFilteredSearch_withUserScope() throws Exception {
        when(searchQueryCorrector.correctQuery("test")).thenReturn("test");
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 2L});
        EmbeddedExpense result = EmbeddedExpense.builder().id(6L).category("Transport").build();
        when(qdrantVectorService.searchWithFilter("test", 15, new QdrantVectorService.SearchFilter(null, null, null, null, null, 2L)))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/v1/search/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"test\", \"topK\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(6));
    }

    @Test
    void testFilteredSearch_withDateRange() throws Exception {
        when(searchQueryCorrector.correctQuery("range")).thenReturn("range");
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        EmbeddedExpense result = EmbeddedExpense.builder().id(7L).build();
        when(qdrantVectorService.searchWithFilter("range", 15, new QdrantVectorService.SearchFilter(null, null, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), null, 1L)))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/v1/search/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"range\", \"topK\": 15, \"from\": \"2024-01-01\", \"to\": \"2024-12-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));
    }

    @Test
    void testSemanticSearchValidationError() throws Exception {
        mockMvc.perform(post("/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"\", \"topK\": 15}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testFilteredSearchValidationError() throws Exception {
        mockMvc.perform(post("/v1/search/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"\", \"topK\": 15}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void preAuthorizeRequiresAuthentication() {
        PreAuthorize preAuthorize = ExpenseSearchController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("isAuthenticated()", preAuthorize.value());
    }
}
