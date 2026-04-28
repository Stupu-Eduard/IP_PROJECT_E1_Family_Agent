package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.service.ExtractionPipelineService;
import com.familie.cheltuieli_familie.service.ExtractionService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExtractionController.class)
class ExtractionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExtractionService extractionService;

    @MockitoBean
    private ExtractionPipelineService orchestrator;

    @Test
    @WithMockUser
    void testOcrPipelineIntegration() throws Exception {
        // 1. Create a fake PDF file in memory
        MockMultipartFile fakePdf = new MockMultipartFile(
                "file",               // The parameter name expected by the controller
                "dummy_statement.pdf",// The original file name
                "application/pdf",    // The content type
                "Fake PDF Content".getBytes() // The content (doesn't matter for this connection test)
        );

        // 2. Simulate sending this file to your new endpoint
        mockMvc.perform(multipart("/v1/extract/process")
                        .file(fakePdf)
                        .param("bank", "revolut")
                        .with(csrf())) // <--- Asta elimină eroarea 403 Forbidden
                        .andExpect(status().isOk());
    }
}
