package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.service.PdfExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PdfExportControllerTest {

    @Mock private PdfExportService pdfExportService;
    @Mock private Authentication   auth;
    @InjectMocks private PdfExportController pdfExportController;

    private byte[] fakePdf;

    @BeforeEach
    void setUp() {
        fakePdf = new byte[]{0x25, 0x50, 0x44, 0x46}; // PDF magic bytes
    }

    @Test
    void exportPdf_returnsOk_withDefaultDates() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), any())).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(null, null, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertArrayEquals(fakePdf, response.getBody());
    }

    @Test
    void exportPdf_returnsOk_withExplicitDates() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), any())).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(
                "2025-05-14", "2026-05-14", auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void exportPdf_filenameContainsDates_whenExplicit() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), any())).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(
                "2026-01-01", "2026-01-31", auth);

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.contains("2026-01-01"));
        assertTrue(disposition.contains("2026-01-31"));
        assertTrue(disposition.contains("evolutie-cheltuieli"));
    }

    @Test
    void exportPdf_filenameContainsTodayDates_whenNoParams() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), any())).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(null, null, auth);

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.contains(LocalDate.now().toString()));
    }

    @Test
    void exportPdf_passesCorrectDatesToService() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), any())).thenReturn(fakePdf);

        pdfExportController.exportPdf("2026-03-01", "2026-03-31", auth);

        verify(pdfExportService).generatePdf(
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 3, 31)),
                eq(auth)
        );
    }

    @Test
    void exportPdf_passesDefaultDatesToService_whenNullParams() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), any())).thenReturn(fakePdf);

        pdfExportController.exportPdf(null, null, auth);

        verify(pdfExportService).generatePdf(
                eq(LocalDate.now().minusDays(6)),
                eq(LocalDate.now()),
                eq(auth)
        );
    }

    @Test
    void exportPdf_returns500_whenServiceThrows() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), any()))
                .thenThrow(new IOException("PDF generation failed"));

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(null, null, auth);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void exportPdf_hasAttachmentDisposition() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), any())).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(null, null, auth);

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.startsWith("attachment"));
    }
}