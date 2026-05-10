package com.proiect.controller;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.familie.cheltuieli_familie.repository.UserSessionRepository;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.proiect.service.AnalyticsAssistant;
import com.proiect.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @MockBean
    private UserSessionRepository userSessionRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsAssistant analyticsAssistant;

    @MockitoBean
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
