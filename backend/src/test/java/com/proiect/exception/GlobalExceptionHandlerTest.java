package com.proiect.exception;

import com.proiect.controller.ExtractionController;
import com.proiect.service.ExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExtractionController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExtractionService extractionService;

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
        when(extractionService.process(any())).thenThrow(new RuntimeException("Simulated internal error for testing"));

        mockMvc.perform(post("/v1/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\": \"test\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Eroare internă: Simulated internal error for testing"));
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
