package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.service.ExtractionPipelineService;
import com.familie.cheltuieli_familie.service.ExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WebMvcTest(controllers = ExtractionController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
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
