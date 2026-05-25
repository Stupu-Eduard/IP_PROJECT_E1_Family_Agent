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
        when(pdfExportService.generatePdf(any(), any(), isNull(), any())).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(null, null, null, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertArrayEquals(fakePdf, response.getBody());
    }

    @Test
    void exportPdf_returnsOk_withExplicitDates() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), isNull(), any())).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(
                "2025-05-14", "2026-05-14", null, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void exportPdf_filenameContainsDates_whenExplicit() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), isNull(), any())).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(
                "2026-01-01", "2026-01-31", null, auth);

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.contains("2026-01-01"));
        assertTrue(disposition.contains("2026-01-31"));
        assertTrue(disposition.contains("evolutie-cheltuieli"));
    }

    @Test
    void exportPdf_filenameContainsTodayDates_whenNoParams() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), isNull(), any())).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(null, null, null, auth);

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.contains(LocalDate.now().toString()));
    }

    @Test
    void exportPdf_passesCorrectDatesToService() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), isNull(), any())).thenReturn(fakePdf);

        pdfExportController.exportPdf("2026-03-01", "2026-03-31", null, auth);

        verify(pdfExportService).generatePdf(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null,
                auth
        );
    }

    @Test
    void exportPdf_passesDefaultDatesToService_whenNullParams() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), isNull(), any())).thenReturn(fakePdf);

        pdfExportController.exportPdf(null, null, null, auth);

        verify(pdfExportService).generatePdf(
                LocalDate.now().minusDays(6),
                LocalDate.now(),
                null,
                auth
        );
    }

    @Test
    void exportPdf_returns500_whenServiceThrows() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), isNull(), any()))
                .thenThrow(new IOException("PDF generation failed"));

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(null, null, null, auth);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void exportPdf_hasAttachmentDisposition() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), isNull(), any())).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(null, null, null, auth);

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.startsWith("attachment"));
    }

    // ── Teste noi: filtrare pe persoană ──────────────────────────────────────

    @Test
    void exportPdf_passesPersonToService_whenPersonProvided() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), eq("Ana Popescu"), any())).thenReturn(fakePdf);

        pdfExportController.exportPdf("2026-01-01", "2026-01-31", "Ana Popescu", auth);

        verify(pdfExportService).generatePdf(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                "Ana Popescu",
                auth
        );
    }

    @Test
    void exportPdf_filenameContainsPersonName_whenPersonProvided() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), eq("Mihai Ionescu"), any())).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(
                "2026-01-01", "2026-01-31", "Mihai Ionescu", auth);

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.contains("Mihai-Ionescu"));
    }

    @Test
    void exportPdf_treatsBlankPersonAsNull() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), isNull(), any())).thenReturn(fakePdf);

        pdfExportController.exportPdf("2026-01-01", "2026-01-31", "   ", auth);

        verify(pdfExportService).generatePdf(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                null,
                auth
        );
    }

    @Test
    void exportPdf_filenameHasNoPersonSuffix_whenPersonIsNull() throws Exception {
        when(pdfExportService.generatePdf(any(), any(), isNull(), any())).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(
                "2026-01-01", "2026-01-31", null, auth);

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.contains("evolutie-cheltuieli-2026-01-01-2026-01-31.pdf"));
    }

    @Test
    void exportPdf_returns500_whenRuntimeExceptionThrown() throws Exception {
        // Acoperă ramura catch(RuntimeException) — distinctă de IOException
        when(pdfExportService.generatePdf(any(), any(), isNull(), any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<byte[]> response = pdfExportController.exportPdf(null, null, null, auth);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void exportPdf_returns500_whenDateFormatInvalid() {
        // DateTimeParseException (extends RuntimeException) aruncată la LocalDate.parse()
        // — acoperă ramura catch fără a fi nevoie de mock pe service
        ResponseEntity<byte[]> response = pdfExportController.exportPdf(
                "not-a-date", "2026-01-31", null, auth);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}