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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private AgentChatService agentChatService;

    @Mock
    private ChatRateLimiter rateLimiter;

    @InjectMocks
    private ChatController chatController;

    @Test
    void chat_shouldReturnResponseWhenAllowed() {
        when(rateLimiter.allowRequest(anyString())).thenReturn(true);
        when(agentChatService.processQuery(anyString())).thenReturn(
                new TextResponseDTO("Hello")
        );

        ChatController.ChatRequest req = new ChatController.ChatRequest();
        req.setMessage("test message");

        ResponseEntity<Object> response = chatController.chat(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void chat_shouldReturnTooManyRequestsWhenRateLimited() {
        when(rateLimiter.allowRequest(anyString())).thenReturn(false);
        when(rateLimiter.getRemainingSeconds(anyString())).thenReturn(30L);

        ChatController.ChatRequest req = new ChatController.ChatRequest();
        req.setMessage("test message");

        ResponseEntity<Object> response = chatController.chat(req);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").contains("Rate limit exceeded"));
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
