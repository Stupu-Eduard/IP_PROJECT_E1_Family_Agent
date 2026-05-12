package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.service.PdfReportService;
import com.familie.cheltuieli_familie.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PdfReportService pdfReportService;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

    @Test
    void testDownloadFinancialReport() throws Exception {
        byte[] pdfContent = "mock pdf content".getBytes();
        when(pdfReportService.generateFinancialReport(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(pdfContent);

        mockMvc.perform(get("/api/v1/reports/financial-pdf")
                .param("from", "2024-01-01")
                .param("to", "2024-01-31")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=financial_report_2024-01-01_to_2024-01-31.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    void testGetNarrativeReport() throws Exception {
        String mockReport = "Mock narrative report";
        when(reportService.generateNarrativeReport(anyInt(), anyInt())).thenReturn(mockReport);

        mockMvc.perform(get("/api/v1/reports/narrative")
                .param("year", "2024")
                .param("month", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(mockReport));
    }
}
