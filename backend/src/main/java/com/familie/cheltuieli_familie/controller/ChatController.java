package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.service.RagRetrievalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/chat")
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final RagRetrievalService ragRetrievalService;

    @Data
    public static class ChatRequest {
        @NotBlank(message = "Message cannot be empty")
        private String message;
    }

    @Data
    public static class ChatResponse {
        private final String reply;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest req) {
        log.info("Chat message received: {}", req.getMessage());
        String reply = ragRetrievalService.askWithContext(req.getMessage());
        return ResponseEntity.ok(new ChatResponse(reply));
    }
}