package com.proiect.controller;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.proiect.dto.ExtractionResponse;
import com.proiect.service.ExtractionService;
import com.proiect.service.PdfExtractionService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.proiect.service.VoiceService;
import java.util.List;

@WebMvcTest(FileUploadController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PdfExtractionService pdfExtractionService;

    @MockBean
    private ExtractionService extractionService;

    @MockBean
    private VoiceService voiceService;

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
    }

    @Test
    void testUploadAudio() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.wav", "audio/wav", "audio content".getBytes());

        when(voiceService.transcribe(any())).thenReturn("Transcript text");

        ExtractionResponse response = ExtractionResponse.builder()
                .amount(new BigDecimal("50.00"))
                .category("Alimente")
                .location("Lidl")
                .person("Eu")
                .transactionDate(LocalDate.now())
                .rawInput("Transcript text")
                .build();
        when(extractionService.process(any())).thenReturn(List.of(response));

        mockMvc.perform(multipart("/v1/upload/audio").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(50.00))
                .andExpect(jsonPath("$[0].person").value("Eu"));
    }

    @Test
    void testUploadImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", MediaType.IMAGE_PNG_VALUE, "image".getBytes());

        mockMvc.perform(multipart("/v1/upload/image").file(file))
                .andExpect(status().isNotImplemented())
                .andExpect(content().string("OCR module is under development by the M5 team. Please use PDF upload instead."));
    }
}
