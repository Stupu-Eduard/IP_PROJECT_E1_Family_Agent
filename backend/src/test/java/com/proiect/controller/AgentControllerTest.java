package com.proiect.controller;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.proiect.dto.response.AgentResponseDTO;
import com.proiect.dto.response.ChartPayload;
import com.proiect.dto.response.ChartResponseDTO;
import com.proiect.dto.response.TextResponseDTO;
import com.proiect.service.AgentChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgentController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentChatService agentChatService;

    @Test
    void testTextResponse() throws Exception {
        when(agentChatService.processQuery("Cât am cheltuit ieri?"))
                .thenReturn(new TextResponseDTO("Ai cheltuit 50 RON ieri."));

        mockMvc.perform(post("/v1/agent/chat")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Cât am cheltuit ieri?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("text"))
                .andExpect(jsonPath("$.message").value("Ai cheltuit 50 RON ieri."));
    }

    @Test
    void testChartResponse() throws Exception {
        ChartPayload payload = ChartPayload.builder()
                .chartType("bar")
                .title("Comparație cheltuieli")
                .data(List.of(
                        Map.of("name", "Teodor", "total", 1200),
                        Map.of("name", "Maria", "total", 890)
                ))
                .dataKeys(List.of("total"))
                .xAxisKey("name")
                .build();

        when(agentChatService.processQuery("Compară-mă cu Maria"))
                .thenReturn(new ChartResponseDTO("Iată comparativul:", payload));

        mockMvc.perform(post("/v1/agent/chat")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Compară-mă cu Maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("chart"))
                .andExpect(jsonPath("$.message").value("Iată comparativul:"))
                .andExpect(jsonPath("$.payload.chartType").value("bar"))
                .andExpect(jsonPath("$.payload.title").value("Comparație cheltuieli"))
                .andExpect(jsonPath("$.payload.data[0].name").value("Teodor"))
                .andExpect(jsonPath("$.payload.data[0].total").value(1200))
                .andExpect(jsonPath("$.payload.dataKeys[0]").value("total"));
    }
}
