package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.response.TextResponseDTO;
import com.familie.cheltuieli_familie.security.ChatRateLimiter;
import com.familie.cheltuieli_familie.service.AgentChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private AgentChatService agentChatService;

    @MockBean
    private com.familie.cheltuieli_familie.security.ChatRateLimiter chatRateLimiter;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

    @Test
    void testChat_returnsTextResponse() throws Exception {
        when(chatRateLimiter.allowRequest(anyString())).thenReturn(true);
        when(agentChatService.processQuery("Cat am cheltuit luna aceasta?"))
                .thenReturn(new TextResponseDTO("Ai cheltuit 1248 de lei luna aceasta."));

        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Cat am cheltuit luna aceasta?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("text"))
                .andExpect(jsonPath("$.text").value("Ai cheltuit 1248 de lei luna aceasta."))
                .andExpect(jsonPath("$.message").value("Ai cheltuit 1248 de lei luna aceasta."));
    }

    @Test
    void testChat_emptyMessage_returnsBadRequest() throws Exception {
        when(chatRateLimiter.allowRequest(anyString())).thenReturn(true);
        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testChat_missingMessage_returnsBadRequest() throws Exception {
        when(chatRateLimiter.allowRequest(anyString())).thenReturn(true);
        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_shouldProcessMessageSuccessfully() {
        when(rateLimiter.allowRequest(anyString())).thenReturn(true);
        TextResponseDTO expectedResponse = new TextResponseDTO("Processed");
        when(agentChatService.processQuery("hello")).thenReturn(expectedResponse);

        ChatController.ChatRequest req = new ChatController.ChatRequest();
        req.setMessage("hello");

        ResponseEntity<Object> response = chatController.chat(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }
}
