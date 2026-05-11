package com.familie.cheltuieli_familie.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionsTest {

    @Test
    void testAiServiceException() {
        AiServiceException ex1 = new AiServiceException("message");
        assertEquals("message", ex1.getMessage());

        Throwable cause = new RuntimeException("cause");
        AiServiceException ex2 = new AiServiceException("message", cause);
        assertEquals("message", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }

    @Test
    void testExternalServiceException() {
        ExternalServiceException ex1 = new ExternalServiceException("message");
        assertEquals("message", ex1.getMessage());

        Throwable cause = new RuntimeException("cause");
        ExternalServiceException ex2 = new ExternalServiceException("message", cause);
        assertEquals("message", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }

    @Test
    void testPipelineException() {
        PipelineException ex1 = new PipelineException("message");
        assertEquals("message", ex1.getMessage());

        Throwable cause = new RuntimeException("cause");
        PipelineException ex2 = new PipelineException("message", cause);
        assertEquals("message", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }

    @Test
    void testResourceInitializationException() {
        ResourceInitializationException ex1 = new ResourceInitializationException("message");
        assertEquals("message", ex1.getMessage());

        Throwable cause = new RuntimeException("cause");
        ResourceInitializationException ex2 = new ResourceInitializationException("message", cause);
        assertEquals("message", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }

    @Test
    void testValidationException() {
        ValidationException ex1 = new ValidationException("message");
        assertEquals("message", ex1.getMessage());

        Throwable cause = new RuntimeException("cause");
        ValidationException ex2 = new ValidationException("message", cause);
        assertEquals("message", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }

    @Test
    void testVectorStoreException() {
        VectorStoreException ex1 = new VectorStoreException("message");
        assertEquals("message", ex1.getMessage());

        Throwable cause = new RuntimeException("cause");
        VectorStoreException ex2 = new VectorStoreException("message", cause);
        assertEquals("message", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }
    
    @Test
    void testAmountNotFoundException() {
        AmountNotFoundException ex = new AmountNotFoundException("message");
        assertEquals("message", ex.getMessage());
    }
}
