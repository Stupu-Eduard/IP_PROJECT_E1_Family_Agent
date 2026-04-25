package com.proiect.controller;

import com.proiect.dto.RagRequest;
import com.proiect.service.RagRetrievalService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/rag")
@Slf4j
@RequiredArgsConstructor
public class RagQueryController {

    private final RagRetrievalService ragRetrievalService;

    @PostMapping("/query")
    public ResponseEntity<String> ragQuery(@Valid @RequestBody RagRequest request) {
        log.info("Received RAG query request: {}", request.getQuery());
        String answer = ragRetrievalService.askWithContext(request.getQuery());
        return ResponseEntity.ok(answer);
    }
}
