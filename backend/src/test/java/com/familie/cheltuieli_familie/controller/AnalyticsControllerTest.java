package com.familie.cheltuieli_familie.controller;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.familie.cheltuieli_familie.service.AnalyticsAssistant;
import com.familie.cheltuieli_familie.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AnalyticsController.class)
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsAssistant analyticsAssistant;

    @MockBean
    private ReportService reportService;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.SessionCookieFilter sessionCookieFilter;

    @Test
    void testQuery() throws Exception {
        when(analyticsAssistant.chat(anyString())).thenReturn("Ai cheltuit 100 lei.");

        mockMvc.perform(post("/v1/analytics/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"Cât am cheltuit?\""))
                .andExpect(status().isOk())
                .andExpect(content().string("Ai cheltuit 100 lei."));
    }

    @Test
    void testGetNarrativeReport() throws Exception {
        when(reportService.generateNarrativeReport(2024, 3)).thenReturn("Raport lunar: ai cheltuit 500 RON.");

        mockMvc.perform(get("/v1/analytics/report/2024/3"))
                .andExpect(status().isOk())
                .andExpect(content().string("Raport lunar: ai cheltuit 500 RON."));
    }
}
