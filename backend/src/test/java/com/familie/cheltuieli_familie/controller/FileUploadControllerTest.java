package com.familie.cheltuieli_familie.controller;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.exception.AiServiceException;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.service.ExtractionService;
import com.familie.cheltuieli_familie.service.OcrService;
import com.familie.cheltuieli_familie.service.PdfExtractionService;
import com.familie.cheltuieli_familie.service.SyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(FileUploadController.class)
@ActiveProfiles("test")
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PdfExtractionService pdfExtractionService;

    @MockBean
    private ExtractionService extractionService;

    @MockBean
    private SyncService syncService;

    @MockBean
    private OcrService ocrService;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

    @Test
    void testUploadPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes());

        when(pdfExtractionService.extractText(any())).thenReturn("Extracted text from PDF");

        ExtractionResponse response = ExtractionResponse.builder()
                .amount(new BigDecimal("100.00"))
                .category("Altele")
                .location("Test")
                .person("Familie")
                .transactionDate(LocalDate.now())
                .rawInput("Extracted text from PDF")
                .build();
        when(extractionService.process(any())).thenReturn(List.of(response));

        mockMvc.perform(multipart("/v1/upload/pdf").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(100.00));

        verify(syncService, times(1)).syncExpense(any(Expense.class));
    }

    @Test
    void testUploadPdf_withOcrFallback() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes());

        when(pdfExtractionService.extractText(any())).thenThrow(new AiServiceException("Empty text"));
        when(ocrService.extractTextFromPdf(any())).thenReturn("OCR extracted text");

        ExtractionResponse response = ExtractionResponse.builder()
                .amount(new BigDecimal("50.00"))
                .category("Altele")
                .location("OCR Test")
                .person("Familie")
                .transactionDate(LocalDate.now())
                .rawInput("OCR extracted text")
                .build();
        when(extractionService.process(any())).thenReturn(List.of(response));

        mockMvc.perform(multipart("/v1/upload/pdf").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(50.00));

        verify(ocrService, times(1)).extractTextFromPdf(any());
        verify(syncService, times(1)).syncExpense(any(Expense.class));
    }

    @Test
    void testUploadAudio() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.wav", "audio/wav", "audio content".getBytes());

        mockMvc.perform(multipart("/v1/upload/audio").file(file))
                .andExpect(status().isNotImplemented())
                .andExpect(content().string("Audio transcription is not available. Please use PDF upload instead."));
    }

    @Test
    void testUploadImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", MediaType.IMAGE_PNG_VALUE, "image".getBytes());

        mockMvc.perform(multipart("/v1/upload/image").file(file))
                .andExpect(status().isNotImplemented())
                .andExpect(content().string("OCR module is under development by the M5 team. Please use PDF upload instead."));
    }
}
