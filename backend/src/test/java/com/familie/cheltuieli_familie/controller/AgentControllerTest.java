package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.response.AgentResponseDTO;
import com.familie.cheltuieli_familie.dto.response.TextResponseDTO;
import com.familie.cheltuieli_familie.service.AgentChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    @Mock
    private AgentChatService agentChatService;

    @InjectMocks
    private AgentController agentController;

    @Test
    void chat_shouldReturnOkResponse() {
        String userMessage = "How much did I spend?";
        TextResponseDTO expectedResponse = new TextResponseDTO("You spent 500 RON.");

        when(agentChatService.processQuery(userMessage)).thenReturn(expectedResponse);

        ResponseEntity<AgentResponseDTO> result = agentController.chat(userMessage);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals("text", result.getBody().getType());
        assertEquals("You spent 500 RON.", result.getBody().getMessage());
        verify(agentChatService).processQuery(userMessage);
    }

    @Test
    void chat_shouldPassMessageToService() {
        String userMessage = "Show me chart";
        when(agentChatService.processQuery(userMessage)).thenReturn(new TextResponseDTO("OK"));

        agentController.chat(userMessage);

        verify(agentChatService).processQuery(userMessage);
    }
}
