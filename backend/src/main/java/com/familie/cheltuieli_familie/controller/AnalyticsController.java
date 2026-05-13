package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.service.AnalyticsAssistant;
import com.familie.cheltuieli_familie.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/analytics")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AnalyticsController {

    private final AnalyticsAssistant analyticsAssistant;
    private final ReportService reportService;

    @PostMapping("/query")
    public ResponseEntity<String> query(@RequestBody String userMessage) {
        log.info("Received analytics query: {}", userMessage);
        String response = analyticsAssistant.chat(userMessage, LocalDate.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/report/{year}/{month}")
    public ResponseEntity<String> getNarrativeReport(@PathVariable int year, @PathVariable int month) {
        log.info("Requesting narrative report for {}/{}", month, year);
        String report = reportService.generateNarrativeReport(year, month);
        return ResponseEntity.ok(report);
    }
}
