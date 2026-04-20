package com.proiect.controller;

import com.proiect.service.RagRetrievalService;
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

@WebMvcTest(RagQueryController.class)
@ActiveProfiles("test")
class RagQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RagRetrievalService ragRetrievalService;

    @Test
    void testRagQuery() throws Exception {
        when(ragRetrievalService.askWithContext("Cât am cheltuit la Mega Image?"))
                .thenReturn("Ai cheltuit 89 de lei la Mega Image.");

        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"Cât am cheltuit la Mega Image?\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Ai cheltuit 89 de lei la Mega Image."));
    }

    @Test
    void testRagQueryValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"\"}"))
                .andExpect(status().isBadRequest());
    }
}
