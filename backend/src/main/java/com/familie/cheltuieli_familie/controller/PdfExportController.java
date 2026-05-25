package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/expenses/export")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PdfExportController {

    private static final DateTimeFormatter PDF_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PdfExportService pdfExportService;

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String person,
            Authentication auth) {
        // S5145 — sanitizare înainte de logare (prevenire log injection)
        log.info("PDF export requested from={} to={} person={}",
                sanitizeForLog(from), sanitizeForLog(to), sanitizeForLog(person));
        try {
            LocalDate fromDate = (from != null) ? LocalDate.parse(from) : LocalDate.now().minusDays(6);
            LocalDate toDate   = (to   != null) ? LocalDate.parse(to)   : LocalDate.now();
            String personFilter = (person != null && !person.isBlank()) ? person : null;

            byte[] pdf = pdfExportService.generatePdf(fromDate, toDate, personFilter, auth);

            String personSuffix = (personFilter != null)
                    ? "-" + personFilter.replaceAll("\\s+", "-")
                    : "";
            String filename = "evolutie-cheltuieli-"
                    + fromDate.format(PDF_DATE_FMT) + "-" + toDate.format(PDF_DATE_FMT)
                    + personSuffix + ".pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(pdf);

        } catch (IOException | RuntimeException e) {
            log.error("Error generating PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Elimină CR/LF din string-uri venite din input utilizator (S5145 — log injection). */
    private static String sanitizeForLog(String value) {
        return value != null ? value.replaceAll("[\r\n\t]", "_") : null;
    }
}