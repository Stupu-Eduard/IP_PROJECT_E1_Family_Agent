package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.service.PdfReportService;
import com.familie.cheltuieli_familie.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final PdfReportService pdfReportService;
    private final ReportService reportService;

    @GetMapping("/financial-pdf")
    public ResponseEntity<byte[]> downloadFinancialReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        log.info("Request received: downloadFinancialReport from {} to {}", from, to);
        try {
            byte[] pdfContent = pdfReportService.generateFinancialReport(from, to);
            log.info("PDF generated successfully, size: {} bytes", pdfContent.length);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financial_report_" + from + "_to_" + to + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfContent);
        } catch (Exception e) {
            log.error("Error generating PDF report for period {} to {}", from, to, e);
            throw e;
        }
    }

    @GetMapping("/narrative")
    public ResponseEntity<String> getNarrativeReport(
            @RequestParam int year,
            @RequestParam int month) {
        String report = reportService.generateNarrativeReport(year, month);
        return ResponseEntity.ok(report);
    }
}
