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
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

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

    @Test
    void chat_requiresAuthentication() {
        PreAuthorize preAuthorize = AgentController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "AgentController should have @PreAuthorize");
        assertEquals("isAuthenticated()", preAuthorize.value());
    }

    // Tests for extractMessage via reflection

    @Test
    void extractMessage_withJsonBodyContainingMessageField_shouldReturnMessage() throws Exception {
        Method method = AgentController.class.getDeclaredMethod("extractMessage", String.class);
        method.setAccessible(true);

        String jsonBody = "{\"message\":\"Hello from JSON\"}";
        String result = (String) method.invoke(agentController, jsonBody);

        assertEquals("Hello from JSON", result);
    }

    @Test
    void extractMessage_withPlainTextBody_shouldReturnRawBody() throws Exception {
        Method method = AgentController.class.getDeclaredMethod("extractMessage", String.class);
        method.setAccessible(true);

        String plainText = "Plain text message";
        String result = (String) method.invoke(agentController, plainText);

        assertEquals(plainText, result);
    }

    @Test
    void extractMessage_withNullBody_shouldReturnNull() throws Exception {
        Method method = AgentController.class.getDeclaredMethod("extractMessage", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(agentController, (String) null);

        assertNull(result);
    }

    @Test
    void extractMessage_withBlankBody_shouldReturnBlankBody() throws Exception {
        Method method = AgentController.class.getDeclaredMethod("extractMessage", String.class);
        method.setAccessible(true);

        String blankBody = "   ";
        String result = (String) method.invoke(agentController, blankBody);

        assertEquals(blankBody, result);
    }

    @Test
    void extractMessage_withJsonWithoutMessageField_shouldReturnRawBody() throws Exception {
        Method method = AgentController.class.getDeclaredMethod("extractMessage", String.class);
        method.setAccessible(true);

        String jsonWithoutMessage = "{\"other\":\"value\"}";
        String result = (String) method.invoke(agentController, jsonWithoutMessage);

        assertEquals(jsonWithoutMessage, result);
    }

    @Test
    void extractMessage_withJsonParseException_shouldReturnRawBody() throws Exception {
        Method method = AgentController.class.getDeclaredMethod("extractMessage", String.class);
        method.setAccessible(true);

        String invalidJson = "{\"message\":\"broken";
        String result = (String) method.invoke(agentController, invalidJson);

        assertEquals(invalidJson, result);
    }

    @Test
    void extractMessage_withJsonMessageFieldAsNumber_shouldReturnRawBody() throws Exception {
        Method method = AgentController.class.getDeclaredMethod("extractMessage", String.class);
        method.setAccessible(true);

        String jsonWithNumberMessage = "{\"message\":12345}";
        String result = (String) method.invoke(agentController, jsonWithNumberMessage);

        assertEquals(jsonWithNumberMessage, result);
    }

    @Test
    void extractMessage_withJsonMessageFieldAsNull_shouldReturnRawBody() throws Exception {
        Method method = AgentController.class.getDeclaredMethod("extractMessage", String.class);
        method.setAccessible(true);

        String jsonWithNullMessage = "{\"message\":null}";
        String result = (String) method.invoke(agentController, jsonWithNullMessage);

        assertEquals(jsonWithNullMessage, result);
    }

    @Test
    void extractMessage_withEmptyString_shouldReturnEmptyString() throws Exception {
        Method method = AgentController.class.getDeclaredMethod("extractMessage", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(agentController, "");

        assertEquals("", result);
    }

    @Test
    void extractMessage_withJsonContainingMessageStringButNotField_shouldReturnRawBody() throws Exception {
        Method method = AgentController.class.getDeclaredMethod("extractMessage", String.class);
        method.setAccessible(true);

        String jsonWithMessageInValue = "{\"other\":\"this has message in value\"}";
        String result = (String) method.invoke(agentController, jsonWithMessageInValue);

        assertEquals(jsonWithMessageInValue, result);
    }

    @Test
    void chat_withJsonBodyInput_shouldExtractMessageAndCallService() {
        String jsonBody = "{\"message\":\"JSON input message\"}";
        TextResponseDTO expectedResponse = new TextResponseDTO("Processed");

        when(agentChatService.processQuery("JSON input message")).thenReturn(expectedResponse);

        ResponseEntity<AgentResponseDTO> result = agentController.chat(jsonBody);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals("Processed", result.getBody().getMessage());
        verify(agentChatService).processQuery("JSON input message");
    }

    @Test
    void chat_withNullBody_shouldPassNullToService() {
        TextResponseDTO expectedResponse = new TextResponseDTO("Null response");

        when(agentChatService.processQuery((String) null)).thenReturn(expectedResponse);

        ResponseEntity<AgentResponseDTO> result = agentController.chat(null);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals("Null response", result.getBody().getMessage());
        verify(agentChatService).processQuery((String) null);
    }

    @Test
    void chat_withBlankBody_shouldPassBlankToService() {
        String blankBody = "   ";
        TextResponseDTO expectedResponse = new TextResponseDTO("Blank response");

        when(agentChatService.processQuery(blankBody)).thenReturn(expectedResponse);

        ResponseEntity<AgentResponseDTO> result = agentController.chat(blankBody);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals("Blank response", result.getBody().getMessage());
        verify(agentChatService).processQuery(blankBody);
    }

    @Test
    void chat_withInvalidJsonBody_shouldPassRawBodyToService() {
        String invalidJson = "{\"message\":\"broken";
        TextResponseDTO expectedResponse = new TextResponseDTO("Raw response");

        when(agentChatService.processQuery(invalidJson)).thenReturn(expectedResponse);

        ResponseEntity<AgentResponseDTO> result = agentController.chat(invalidJson);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals("Raw response", result.getBody().getMessage());
        verify(agentChatService).processQuery(invalidJson);
    }
}
