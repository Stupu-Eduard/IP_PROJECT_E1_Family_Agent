package com.familie.cheltuieli_familie.controller;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.service.QdrantVectorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

    @Test
    void testSemanticSearch() throws Exception {
        EmbeddedExpense result = EmbeddedExpense.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .category("Mâncare")
                .rawInput("Am platit 100 lei")
                .score(0.95)
                .build();

        when(qdrantVectorService.searchSimilar("mancare", 5)).thenReturn(List.of(result));

        mockMvc.perform(post("/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"mancare\", \"topK\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testFilteredSearch() throws Exception {
        EmbeddedExpense result = EmbeddedExpense.builder()
                .id(2L)
                .amount(new BigDecimal("200.00"))
                .category("Transport")
                .person("Teodor")
                .date(LocalDate.of(2024, 3, 15))
                .rawInput("Am platit 200 lei la benzinărie")
                .score(0.88)
                .build();

        when(qdrantVectorService.searchWithFilter(any(), anyInt(), any(), any(), any(), any()))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/v1/search/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"benzina\", \"topK\": 5, \"category\": \"Transport\", \"person\": \"Teodor\", \"from\": \"2024-03-01\", \"to\": \"2024-03-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Transport"));
    }

    @Test
    void testSemanticSearchValidationError() throws Exception {
        mockMvc.perform(post("/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"\", \"topK\": 5}"))
                .andExpect(status().isBadRequest());
    }
}
