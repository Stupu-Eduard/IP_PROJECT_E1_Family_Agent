package com.familie.cheltuieli_familie.exception;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.familie.cheltuieli_familie.controller.ExtractionController;
import com.familie.cheltuieli_familie.service.ExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ExtractionController.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExtractionService extractionService;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.SessionCookieFilter sessionCookieFilter;

    @Test
    void testAmountNotFoundException() throws Exception {
        when(extractionService.process(any())).thenThrow(new AmountNotFoundException("Suma nu a fost găsită"));

        mockMvc.perform(post("/v1/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\": \"test\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Suma nu a fost găsită"));
    }

    @Test
    void testPipelineException() throws Exception {
        when(extractionService.process(any())).thenThrow(new PipelineException("Pipeline failed"));

        mockMvc.perform(post("/v1/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\": \"test\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Pipeline failed"));
    }

    @Test
    void testGenericException() throws Exception {
        when(extractionService.process(any())).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/v1/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\": \"test\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Eroare internă: Unexpected error"));
    }

    @Test
    void testValidationException() throws Exception {
        mockMvc.perform(post("/v1/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed: rawText: Raw text is required and cannot be empty"));
    }
}
