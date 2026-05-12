package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.response.TextResponseDTO;
import com.familie.cheltuieli_familie.service.AgentChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ChatController.class)
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentChatService agentChatService;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

    @Test
    void testChat_returnsTextResponse() throws Exception {
        when(agentChatService.processQuery("Cat am cheltuit luna aceasta?"))
                .thenReturn(new TextResponseDTO("Ai cheltuit 1248 de lei luna aceasta."));

        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Cat am cheltuit luna aceasta?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("text"))
                .andExpect(jsonPath("$.message").value("Ai cheltuit 1248 de lei luna aceasta."));
    }

    @Test
    void testChat_emptyMessage_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testChat_missingMessage_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testChat_requiresAuthentication() {
        PreAuthorize preAuthorize = ChatController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "ChatController should have @PreAuthorize");
        assertEquals("isAuthenticated()", preAuthorize.value());
    }
}
