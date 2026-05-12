package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.service.ExtractionPipelineService;
import com.familie.cheltuieli_familie.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = ExtractionOCRController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@ActiveProfiles("test")
class ExtractionOCRControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExtractionPipelineService extractionPipelineService;

    @MockBean
    private StorageService storageService;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

    @Test
    void extractAndSave_ShouldProcessFileAndReturnTransactions() throws Exception {
        MockMultipartFile fakePdf = new MockMultipartFile(
                "file",
                "extras.pdf",
                "application/pdf",
                "Fisier PDF de test".getBytes()
        );

        Transaction mockTransaction = new Transaction(
                LocalDate.of(2025, 4, 15),
                "Factura Curent",
                250.50,
                "RON",
                "EXPENSE"
        );
        List<Transaction> mockTransactions = List.of(mockTransaction);

        when(extractionPipelineService.processDocument(any(File.class))).thenReturn(mockTransactions);
        when(storageService.save(any())).thenReturn(new StorageResult(1, 1, 0));

        mockMvc.perform(multipart("/api/ocr/extract-and-save")
                        .file(fakePdf)
                        .param("bank", "ing")) // Parametrul este optional in controllerul tau
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(250.50))
                .andExpect(jsonPath("$[0].description").value("Factura Curent"))
                .andExpect(jsonPath("$[0].currency").value("RON"));
    }

    @Test
    void extractAndSave_ShouldHandleServiceExceptions() throws Exception {
        MockMultipartFile fakePdf = new MockMultipartFile(
                "file",
                "test_error.pdf",
                "application/pdf",
                "Fisier defect".getBytes()
        );

        when(extractionPipelineService.processDocument(any(File.class)))
                .thenThrow(new RuntimeException("OCR a esuat brusc!"));

        mockMvc.perform(multipart("/api/ocr/extract-and-save")
                        .file(fakePdf))
                .andExpect(status().isInternalServerError());
    }
}
