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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private AgentChatService agentChatService;

    @Mock
    private ChatRateLimiter rateLimiter;

    @InjectMocks
    private ChatController chatController;

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

    @Test
    void chat_returnsTextResponse() {
        when(rateLimiter.allowRequest(anyString())).thenReturn(true);
        TextResponseDTO expectedResponse = new TextResponseDTO("Ai cheltuit 1248 de lei luna aceasta.");
        when(agentChatService.processQuery("Cat am cheltuit luna aceasta?")).thenReturn(expectedResponse);

        ChatController.ChatRequest req = new ChatController.ChatRequest();
        req.setMessage("Cat am cheltuit luna aceasta?");

        ResponseEntity<Object> response = chatController.chat(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }

    @Test
    void chat_rateLimitExceeded_returnsTooManyRequests() {
        when(rateLimiter.allowRequest(anyString())).thenReturn(false);
        when(rateLimiter.getRemainingSeconds(anyString())).thenReturn(30L);

        ChatController.ChatRequest req = new ChatController.ChatRequest();
        req.setMessage("hello");

        ResponseEntity<Object> response = chatController.chat(req);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    }
}