package com.familie.cheltuieli_familie.controller;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.familie.cheltuieli_familie.service.RagRetrievalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RagQueryController.class)
@ActiveProfiles("test")
class RagQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RagRetrievalService ragRetrievalService;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

    @Test
    void testRagQuery() throws Exception {
        when(ragRetrievalService.askWithContext("intrebare")).thenReturn("Raspuns RAG");

        mockMvc.perform(post("/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"intrebare\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Raspuns RAG"));
    }

    @Test
    void testRagQueryValidationError() throws Exception {
        mockMvc.perform(post("/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"\"}"))
                .andExpect(status().isBadRequest());
    }
}
