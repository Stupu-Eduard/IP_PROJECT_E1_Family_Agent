package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.service.ExtractionPipelineService;
import com.familie.cheltuieli_familie.service.ExtractionService;
import com.familie.cheltuieli_familie.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExtractionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExtractionService extractionService;

    @MockBean
    private StorageService storageService;

    @MockBean
    private ExtractionPipelineService orchestrator;

    @Test
    @WithMockUser
    void testOcrPipelineIntegration() throws Exception {
        MockMultipartFile fakePdf = new MockMultipartFile(
                "file",               // The parameter name expected by the controller
                "dummy_statement.pdf",// The original file name
                "application/pdf",    // The content type
                "Fake PDF Content".getBytes() // The content
        );

        mockMvc.perform(multipart("/v1/extract/process")
                        .file(fakePdf)
                        .param("bank", "revolut")
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
