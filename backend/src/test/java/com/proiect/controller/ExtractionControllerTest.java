package com.proiect.controller;
import org.springframework.boot.test.context.SpringBootTest;
import com.familie.cheltuieli_familie.repository.UserSessionRepository;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.proiect.dto.ExtractionRequest;
import com.proiect.dto.ExtractionResponse;
import com.proiect.service.ExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class ExtractionControllerTest {

    @MockBean
    private UserSessionRepository userSessionRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExtractionService extractionService;

    @Test
    void testExtractDetails() throws Exception {
        ExtractionResponse response = ExtractionResponse.builder()
                .amount(new BigDecimal("150.00"))
                .category("Mâncare")
                .location("Kaufland")
                .person("Familie")
                .transactionDate(LocalDate.now())
                .rawInput("Am platit 150 lei")
                .build();

        when(extractionService.process(any(ExtractionRequest.class))).thenReturn(List.of(response));

        mockMvc.perform(post("/v1/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\": \"Am platit 150 lei\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(150.00))
                .andExpect(jsonPath("$[0].category").value("Mâncare"));
    }

    @Test
    void testValidateOcr() throws Exception {
        when(extractionService.validateOcrContent("raw ocr text")).thenReturn("VALID");

        mockMvc.perform(post("/v1/extract/validate-ocr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("raw ocr text"))
                .andExpect(status().isOk())
                .andExpect(content().string("VALID"));
    }

    @Test
    void testExtractValidationError() throws Exception {
        mockMvc.perform(post("/v1/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\": \"\"}"))
                .andExpect(status().isBadRequest());
    }
}
