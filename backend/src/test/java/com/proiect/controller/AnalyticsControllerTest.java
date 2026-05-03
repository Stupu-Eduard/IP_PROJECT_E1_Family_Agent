package com.proiect.controller;

import com.proiect.service.AnalyticsAssistant;
import com.proiect.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsAssistant analyticsAssistant;

    @MockBean
    private ReportService reportService;

    @Test
    void testQuery() throws Exception {
        when(analyticsAssistant.chat(contains("Cât am cheltuit"))).thenReturn("Ai cheltuit 500 RON.");

        mockMvc.perform(post("/v1/analytics/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"Cât am cheltuit total?\""))
                .andExpect(status().isOk())
                .andExpect(content().string("Ai cheltuit 500 RON."));
    }
}
