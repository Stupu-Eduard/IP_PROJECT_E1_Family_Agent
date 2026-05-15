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

    private final PdfExportService pdfExportService;

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            Authentication auth) {
        log.info("PDF export requested from={} to={}", from, to);
        try {
            LocalDate fromDate = (from != null) ? LocalDate.parse(from) : LocalDate.now().minusDays(6);
            LocalDate toDate   = (to   != null) ? LocalDate.parse(to)   : LocalDate.now();

            byte[] pdf = pdfExportService.generatePdf(fromDate, toDate, auth);

            String filename = "evolutie-cheltuieli-"
                    + fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    + "-" + toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    + ".pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(pdf);

        } catch (IOException e) {
            log.error("Error generating PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}