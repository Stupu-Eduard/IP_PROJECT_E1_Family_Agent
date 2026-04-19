package com.proiect.controller;

import com.proiect.service.AnalyticsAssistant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/analytics")
@Slf4j
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsAssistant analyticsAssistant;

    @PostMapping("/query")
    public ResponseEntity<String> query(@RequestBody String userMessage) {
        log.info("Received analytics query: {}", userMessage);
        // We pass the current date for context in the prompt
        String response = analyticsAssistant.chat(userMessage + " (Today's date is " + LocalDate.now() + ")");
        return ResponseEntity.ok(response);
    }
}
