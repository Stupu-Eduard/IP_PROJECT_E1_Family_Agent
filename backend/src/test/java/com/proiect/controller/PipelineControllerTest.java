package com.proiect.controller;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.proiect.service.ExpensePipelineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(PipelineController.class)
@ActiveProfiles("test")
class PipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpensePipelineService pipelineService;

    @Test
    void testProcess() throws Exception {
        when(pipelineService.processRawInput("Am platit 100 lei")).thenReturn(List.of(1L));

        mockMvc.perform(post("/v1/pipeline/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\": \"Am platit 100 lei\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(1));
    }

    @Test
    void testProcessValidationError() throws Exception {
        mockMvc.perform(post("/v1/pipeline/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\": \"\"}"))
                .andExpect(status().isBadRequest());
    }
}
