package com.familie.cheltuieli_familie.controller;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.familie.cheltuieli_familie.dto.ExtractionRequest;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.service.ExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = ExtractionController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@ActiveProfiles("test")
class ExtractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExtractionService extractionService;

    @MockBean
    private com.familie.cheltuieli_familie.service.StorageService storageService;

    @MockBean
    private com.familie.cheltuieli_familie.service.ExtractionPipelineService extractionPipelineService;

    @MockBean
    private dev.langchain4j.model.chat.ChatLanguageModel chatLanguageModel;

    @MockBean
    private com.familie.cheltuieli_familie.service.SyncService syncService;

    @MockBean
    private com.familie.cheltuieli_familie.service.TextBasedPdfExtractor textExtractor;

    @MockBean
    private com.familie.cheltuieli_familie.service.BankOcrService bankOcrService;

    @MockBean
    private com.familie.cheltuieli_familie.service.BankStatementParser bankStatementParser;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

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

        verify(syncService, times(1)).syncExpense(any(Expense.class));
    }

    @Test
    void testValidateOcr() throws Exception {
        when(extractionService.validateOcrContent(anyString())).thenReturn("VALID");
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