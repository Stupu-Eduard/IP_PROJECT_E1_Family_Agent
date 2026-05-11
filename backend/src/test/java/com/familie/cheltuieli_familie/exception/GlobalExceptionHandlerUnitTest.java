package com.familie.cheltuieli_familie.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerUnitTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testHandleAmountNotFoundException() {
        AmountNotFoundException ex = new AmountNotFoundException("Amount not found");
        ResponseEntity<Object> response = handler.handleAmountNotFoundException(ex);
        
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Amount not found", body.get("message"));
        assertEquals(422, body.get("status"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandlePipelineException() {
        PipelineException ex = new PipelineException("Pipeline failed");
        ResponseEntity<Object> response = handler.handlePipelineException(ex);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Pipeline failed", body.get("message"));
        assertEquals(500, body.get("status"));
    }

    @Test
    void testHandleAiServiceException() {
        AiServiceException ex = new AiServiceException("AI error");
        ResponseEntity<Object> response = handler.handleAiServiceException(ex);
        
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("AI Service Error: AI error", body.get("message"));
        assertEquals(503, body.get("status"));
    }

    @Test
    void testHandleVectorStoreException() {
        VectorStoreException ex = new VectorStoreException("Vector error");
        ResponseEntity<Object> response = handler.handleVectorStoreException(ex);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Vector Store Error: Vector error", body.get("message"));
        assertEquals(500, body.get("status"));
    }

    @Test
    void testHandleResourceInitializationException() {
        ResourceInitializationException ex = new ResourceInitializationException("Init error");
        ResponseEntity<Object> response = handler.handleResourceInitializationException(ex);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Initialization Error: Init error", body.get("message"));
        assertEquals(500, body.get("status"));
    }

    @Test
    void testHandleExternalServiceException() {
        ExternalServiceException ex = new ExternalServiceException("External error");
        ResponseEntity<Object> response = handler.handleExternalServiceException(ex);
        
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("External Service Error: External error", body.get("message"));
        assertEquals(502, body.get("status"));
    }

    @Test
    void testHandleCustomValidationException() {
        ValidationException ex = new ValidationException("Validation error");
        ResponseEntity<Object> response = handler.handleCustomValidationException(ex);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Validation Error: Validation error", body.get("message"));
        assertEquals(400, body.get("status"));
    }

    @Test
    void testHandleValidationException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "default message");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        
        ResponseEntity<Object> response = handler.handleValidationException(ex);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Validation failed: field: default message", body.get("message"));
        assertEquals(400, body.get("status"));
    }

    @Test
    void testHandleGenericException() {
        Exception ex = new Exception("Generic exception");
        ResponseEntity<Object> response = handler.handleGenericException(ex);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Eroare internă: Generic exception", body.get("message"));
        assertEquals(500, body.get("status"));
    }
}
